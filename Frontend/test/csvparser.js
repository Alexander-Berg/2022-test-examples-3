/* global describe, it, CsvParser */
const assert = require('assert');
const fs = require('fs');
const path = require('path');
const vm = require('vm');

function load(file) {
  const filename = path.join(__dirname, file);
  const code = fs.readFileSync(filename).toString();
  new vm.Script(code, { filename, displayErrors: true }).runInThisContext();
}

load('../src/csvparser.js');

describe('CSV parser', function() {
  it('should parse former quoted format', function() {
    const csvParser = new CsvParser();

    assert.deepStrictEqual(csvParser.parseLine('1,"a,b,c",2'), ['1', 'a,b,c', '2']);
    assert.deepStrictEqual(csvParser.parseLine('1,2,"a,b,c"'), ['1', '2', 'a,b,c']);

    assert.deepStrictEqual(
      csvParser.parseLine('shared-library,"/usr/lib/libc++.1.dylib",0x7fff726ea750,0x7fff727329f8,498774016'),
      ['shared-library', '/usr/lib/libc++.1.dylib', '0x7fff726ea750', '0x7fff727329f8', '498774016']
    );

    assert.deepStrictEqual(
      csvParser.parseLine('code-creation,RegExp,6,10482372,0x2f9678893a0,947,"^[A-Za-z0-9][A-Za-z0-9!#$&^_.-]{0\\,126}$"'),
      ['code-creation', 'RegExp', '6', '10482372', '0x2f9678893a0', '947', '^[A-Za-z0-9][A-Za-z0-9!#$&^_.-]{0\\,126}$']
    );
  });

  it('should parse actual unquoted format', function() {
    const csvParser = new CsvParser();

    assert.deepStrictEqual(
      csvParser.parseLine('shared-library,/usr/lib/libc++.1.dylib,0x7fff726ea750,0x7fff727329f8,498774016'),
      ['shared-library', '/usr/lib/libc++.1.dylib', '0x7fff726ea750', '0x7fff727329f8', '498774016']
    );

    assert.deepStrictEqual(
      csvParser.parseLine('code-creation,RegExp,4,87522,0xc65d9102f20,1008,^[A-Za-z0-9][A-Za-z0-9!#$&^_-]{0\\x2C126}$'),
      ['code-creation', 'RegExp', '4', '87522', '0xc65d9102f20', '1008', '^[A-Za-z0-9][A-Za-z0-9!#$&^_-]{0,126}$']
    );
  });
});
