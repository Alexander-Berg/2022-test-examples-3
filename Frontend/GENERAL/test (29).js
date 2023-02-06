const fs = require('fs');
const { echo, exec, exit, rm } = require('shelljs');

function execWithCode(expected, ...args) {
  const pipe = exec(...args);
  const { code } = pipe;
  if (code !== expected) {
    echo('Wrong status code:', code);
    exit(1);
  }
  return pipe;
}

execWithCode(2, 'bin/ynode-tick-processor --help');

rm('-f', '*-v8*.log', 'v8.json');

// execWithCode(0, 'ynode --prof --log-all examples/mandelbrot 200 1', { silent: true });
execWithCode(0, 'node --prof --log-all examples/mandelbrot 200 1', { silent: true });
execWithCode(0, 'bin/ynode-tick-processor --preprocess *-v8*.log', { silent: true }).to('v8.json');

/*
 It is OK to have in console error messages like this one:
 "/Library/Developer/CommandLineTools/usr/bin/nm: /usr/lib/libc++.1.dylib: File format has no dynamic symbol table."

 Console output should contain demangled names for C++ code. Name column should not contain separate "T" or "t" symbols.

 Good:
 [C++]:
   ticks  total  nonlib   name
     31   27.9%   28.7%  node::(anonymous namespace)::ContextifyScript::New(v8::FunctionCallbackInfo<v8::Value> const&)

 Bad:
 [C++]:
   ticks  total  nonlib   name
     31   27.9%   28.7%  t __ZN4node12_GLOBAL__N_116ContextifyScript3NewERKN2v820FunctionCallbackInfoINS2_5ValueEEE
 */
const reportFile = 'v8-test-output.txt';
execWithCode(0, 'bin/ynode-tick-processor *-v8*.log').to(reportFile);
const reportLines = fs.readFileSync(reportFile).toString().split('\n');
let hasErrors = false;

const wrongNmParse = reportLines.filter(line => /%\s+t \w/i.test(line));
if (wrongNmParse.length > 0) {
  console.error('\nC++ entries are improperly parsed. There should be no alone "t" symbol before the name:\n' +
    wrongNmParse.slice(0, 5).join('\n') +
    `\nSee ${reportFile} for details.`);
  hasErrors = true;
}

const wrongNmDemangle = reportLines.filter(line => / __Z/.test(line));
if (wrongNmDemangle.length > 0) {
  console.error('\nC++ entries are improperly demangled:\n' +
    wrongNmDemangle.slice(0, 5).join('\n') +
    `\nSee ${reportFile} for details.`);
  hasErrors = true;
}

if (!hasErrors) {
  rm('-f', reportFile);
}

rm('-f', '*-v8*.log', 'v8.json');

if (hasErrors) {
  process.exit(1);
}
