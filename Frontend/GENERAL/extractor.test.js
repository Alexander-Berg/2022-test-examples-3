'use strict';

const assert = require('chai').assert;
const sinon = require('sinon');

const exampleExtractor = require('./extractor');
const parse = exampleExtractor.parse;

sinon.assert.expose(assert, { prefix: '' });

describe('serpdocs', function() {
    let exampleContent;
    let parsedExample;

    it('should extract caption from \'caption\' tag', function() {
        exampleContent = '<caption>описание примера</caption>';
        parsedExample = parse(exampleContent);

        assert.equal(parsedExample.caption, 'описание примера');
    });

    describe('when extract inline example', function() {
        beforeEach(function() {
            exampleContent = 'описание примера\nкод примера';
            parsedExample = parse(exampleContent);
        });

        it('should get caption', function() {
            assert.equal(parsedExample.caption, 'описание примера');
        });

        it('should get code', function() {
            assert.equal(parsedExample.code, 'код примера');
        });

        it('should get empty path', function() {
            assert.equal(parsedExample.path, '');
        });
    });

    describe('when extract non inlined example', function() {
        beforeEach(function() {
            exampleContent = 'описание примера\n{@path:ololo.json}';
            parsedExample = parse(exampleContent);
        });

        it('should get caption', function() {
            assert.equal(parsedExample.caption, 'описание примера');
        });

        it('should get empty code', function() {
            assert.equal(parsedExample.code, '');
        });

        it('should get example filename', function() {
            assert.equal(parsedExample.path, 'ololo.json');
        });
    });
});
