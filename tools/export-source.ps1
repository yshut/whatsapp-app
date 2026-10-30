param([Parameter(Mandatory=$true)][string]$OutputPath)
$ErrorActionPreference='Stop'
$projectRoot=Split-Path -Parent $PSScriptRoot
$outputFile=[IO.Path]::GetFullPath($OutputPath)
if(Test-Path -LiteralPath $outputFile){throw 'Destination exists; choose another filename.'}
if(Test-Path -LiteralPath ($outputFile+'.sha256')){throw 'Checksum destination already exists.'}
$outputParent=Split-Path -Parent $outputFile
if(-not(Test-Path -LiteralPath $outputParent -PathType Container)){throw 'Output parent directory must exist.'}
$inspectionText=& node (Join-Path $PSScriptRoot 'check-repository.mjs') --list-json
if($LASTEXITCODE -ne 0){throw 'Source inspection failed; no archive written.'}
$inspection=($inspectionText -join "`n") | ConvertFrom-Json
Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem
$stream=[IO.File]::Open($outputFile,[IO.FileMode]::CreateNew)
$zip=New-Object IO.Compression.ZipArchive($stream,[IO.Compression.ZipArchiveMode]::Create)
$manifest=@()
try {
 foreach($relative in $inspection.files){
  $source=[IO.Path]::GetFullPath((Join-Path $projectRoot $relative))
  if(-not $source.StartsWith($projectRoot+[IO.Path]::DirectorySeparatorChar,[StringComparison]::OrdinalIgnoreCase)){throw 'Source outside project root.'}
  $hash=(Get-FileHash -LiteralPath $source -Algorithm SHA256).Hash.ToLowerInvariant()
  [IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zip,$source,$relative,[IO.Compression.CompressionLevel]::Optimal) | Out-Null
  $manifest+=@{path=$relative;sha256=$hash;bytes=(Get-Item -LiteralPath $source).Length}
 }
 $entry=$zip.CreateEntry('source-manifest.json')
 $writer=New-Object IO.StreamWriter($entry.Open(),(New-Object Text.UTF8Encoding($false)))
 try {$writer.Write((@{format=1;files=$manifest;gitHistoryIncluded=$false}|ConvertTo-Json -Depth 5))}finally{$writer.Dispose()}
}finally{$zip.Dispose();$stream.Dispose()}
$readZip=[IO.Compression.ZipFile]::OpenRead($outputFile)
try {
 foreach($record in $manifest){
  $item=$readZip.GetEntry($record.path)
  if($null -eq $item -or $item.Length -ne $record.bytes){throw 'Archive entry mismatch.'}
  $entryStream=$item.Open();$sha=[Security.Cryptography.SHA256]::Create()
  try {$actual=([BitConverter]::ToString($sha.ComputeHash($entryStream))).Replace('-','').ToLowerInvariant()}finally{$sha.Dispose();$entryStream.Dispose()}
  if($actual -ne $record.sha256){throw 'Archive hash mismatch.'}
 }
}finally{$readZip.Dispose()}
$archiveHash=(Get-FileHash -LiteralPath $outputFile -Algorithm SHA256).Hash.ToLowerInvariant()
[IO.File]::WriteAllText($outputFile+'.sha256',$archiveHash+'  '+[IO.Path]::GetFileName($outputFile)+"`n",(New-Object Text.UTF8Encoding($false)))
[pscustomobject]@{status='PASS';files=$manifest.Count;archive=$outputFile;bytes=(Get-Item -LiteralPath $outputFile).Length;sha256=$archiveHash;historyIncluded=$false}|ConvertTo-Json
