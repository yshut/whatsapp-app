import fs from 'node:fs';
import path from 'node:path';
import {execFileSync} from 'node:child_process';
import {fileURLToPath} from 'node:url';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const git=(args)=>execFileSync('git',args,{cwd:root,encoding:'utf8',stdio:['ignore','pipe','pipe']});
try {
 const gitRoot=path.resolve(git(['rev-parse','--show-toplevel']).trim());
 if(gitRoot.toLowerCase()!==root.toLowerCase())throw new Error('Run inside this project Git repository.');
 const files=[...new Set(git(['ls-files','--cached','--others','--exclude-standard','-z']).split('\0').filter(Boolean))].filter(p=>fs.existsSync(path.join(root,p))).sort();
 const findings=[];
 const contentRules=[
  ['local-user-path',/C:[\\/]+Users[\\/]+/i],
  ['proxy-endpoint',/novproxy\.io/i],
  ['private-key',/-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----/],
  ['credential-token',/(?:ghp_[A-Za-z0-9]{30,}|github_pat_[A-Za-z0-9_]{30,}|sk-proj-[A-Za-z0-9_-]{30,}|\b\d{8,12}:[A-Za-z0-9_-]{30,})/]
 ];
 for(const file of files){
  const full=path.resolve(root,file);
  if(!full.startsWith(root+path.sep)||fs.lstatSync(full).isSymbolicLink()){findings.push({file,rule:'outside-source-or-symlink'});continue;}
  if(/(?:^|\/)(?:build|\.gradle|\.git|node_modules|\.cxx|apk)(?:\/|$)/.test(file)||/\.(?:apk|apks|aab|jks|keystore|p12|pfx|pem|log|zip)$/i.test(file)||/(?:^|\/)(?:local\.properties|key\.properties|keystore\.properties|residential-import\.json|residential-config\.enc|network-options\.json|\.env(?:\..*)?)$/.test(file)&&!file.endsWith('.env.example'))findings.push({file,rule:'private-or-generated-file'});
  const bytes=fs.readFileSync(full);if(bytes.includes(0))continue;
  const text=bytes.toString('utf8');
  for(const [rule,regex]of contentRules){const match=regex.exec(text);if(match)findings.push({file,line:text.slice(0,match.index).split('\n').length,rule});}
 }
 const required=['README.md','LICENSE','manager/build.gradle','Bcore/build.gradle','gradle/wrapper/gradle-wrapper.jar','Bcore/src/main/cpp/Dobby/arm64-v8a/libdobby.a','.github/workflows/build.yml','build-app.ps1'];
 for(const file of required)if(!files.includes(file))findings.push({file,rule:'required-source-missing'});
 const summary={status:findings.length?'FAIL':'PASS',fileCount:files.length,findings,historyScanned:false};
 if(findings.length){console.error(JSON.stringify(summary,null,2));process.exitCode=1;}
 else console.log(JSON.stringify(process.argv.includes('--list-json')?{summary,files}:summary,null,2));
}catch(error){console.error(JSON.stringify({status:'FAIL',reason:error.message.split('\n')[0]}));process.exitCode=1;}
