/* global describe, it, WebInspector */
const assert = require('assert');
const fs = require('fs');
const path = require('path');
const vm = require('vm');

function load(file) {
  const filename = path.join(__dirname, file);
  const code = fs.readFileSync(filename).toString();
  new vm.Script(code, { filename, displayErrors: true }).runInThisContext();
}

load('../src/sourcemap.js');

describe('SourceMap', function() {
  describe('_decodeVLQ', function() {
    it('should decode negative numbers', function() {
      const sm = new WebInspector.SourceMap(null, {
        sources: [],
        mappings: 'D'
      });
      // mappings: 'D' decodes to columnNumber: -1
      const [, columnNumber] = sm._mappings[0];
      assert.strictEqual(columnNumber, -1);
    });
  });
});
