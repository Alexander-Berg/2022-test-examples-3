'use strict';

const assert = require('chai').assert;
const sinon = require('sinon');

const { getBlockName, buildPatchPath } = require('./naming-helpers');

sinon.assert.expose(assert, { prefix: '' });

describe('patch examples', function() {
    describe('getBlockName', function() {
        it('should extract block name from underscore synax', function() {
            assert.equal(getBlockName('fold_ololo.example.json'), 'fold');
        });

        it('should extract block name from dot synax', function() {
            assert.equal(getBlockName('fold.ololo.example.json'), 'fold');
        });
    });

    describe('buildPatchPath', function() {
        it('should create relative path', function() {
            const name = 'fold.ololo.example.json';
            const expectedPath = 'construct/common/fold/fold.examples/fold.ololo.example.json';

            assert.equal(buildPatchPath(name, 'common'), expectedPath);
        });

        it('should append set default extension', function() {
            const name = 'fold.ololo.example';
            const expectedPath = 'construct/common/fold/fold.examples/fold.ololo.example.json';

            assert.equal(buildPatchPath(name, 'common'), expectedPath);
        });

        it('should save extension if exists', function() {
            const name = 'fold.ololo.example.js';
            const expectedPath = 'construct/common/fold/fold.examples/fold.ololo.example.js';

            assert.equal(buildPatchPath(name, 'common'), expectedPath);
        });
    });
});
