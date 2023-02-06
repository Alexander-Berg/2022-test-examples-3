import fs from 'fs';
import chai from 'chai';
import sinon, { assert } from 'sinon';
import _ from 'lodash';

import HermioneAstParser from '../../../src/parsers/tide-hermione-parser/parser';
import { Test, Tide } from '../../../src';
import TestFile from '../../../src/test-file';

const raw = `'use strict';
specs({
    feature: 'Feature-name'
}, () => {
    describe('Describe 1', () => {
        beforeEach(async function () {
            await this.browser.yaOpenSerp({ text: 'text'}, this.PO.serpList());
        });

        it('It 1', async function () {
            await this.browser.assertView('plane', this.PO.serpList());
            await this.browser.assertView('plane 2', this.PO.serpList());
        });

        it('It 2', async function () {
            await this.browser.assertView('plane', this.PO.serpList());
            await this.browser.assertView('plane 2', this.PO.serpList());
        });
    });

    it('It 3', async function () {
        await this.browser.yaOpenSerp({ text: 'text'}, this.PO.serpList());
        await this.browser.assertView('plane', this.PO.serpList());
    });
});`;
const mkTideStub = (): Tide =>
    ({
        fileCollection: {
            addFile: sinon.stub(),
            getFile: sinon.stub(),
        },
        testCollection: {
            addTest: sinon.stub(),
        },
        constants: { TYPES: {} },
    } as unknown as Tide);

describe('tide-hermione-parser / ast-parser', () => {
    let tide: Tide;
    let parser: HermioneAstParser;
    const filePath = 'some-filepath';

    beforeEach(() => {
        tide = mkTideStub();

        parser = new HermioneAstParser();

        sinon.stub(fs.promises, 'readFile').withArgs(filePath).resolves(raw);
    });

    describe('read', () => {
        beforeEach(() => {
            sinon.stub(parser, 'getTitlePaths').returns([['test 1'], ['test 2']]);
            sinon.stub(TestFile.prototype as any, 'constructor');
            sinon.stub(parser, 'parser').returns({});
        });

        it('should read and parse hermione file', async () => {
            await parser.read(tide, filePath);

            assert.calledOnce(fs.promises.readFile as any);
            assert.calledOnce(parser.parser);
        });

        it('should add file to fileCollection', async () => {
            await parser.read(tide, filePath);

            assert.calledOnce(tide.fileCollection.addFile as any);
        });

        it('should add tests to testCollection', async () => {
            await parser.read(tide, filePath);

            assert.calledTwice(tide.testCollection.addTest as any);
        });
    });

    describe('getTitlePaths', () => {
        it('should return test title paths', async () => {
            const expected = [
                [{ feature: 'Feature-name' }, 'Describe 1', 'It 1'],
                [{ feature: 'Feature-name' }, 'Describe 1', 'It 2'],
                [{ feature: 'Feature-name' }, 'It 3'],
            ];
            const ast = parser.parser(raw);

            const actual = parser.getTitlePaths({ ast } as unknown as TestFile);

            chai.assert.deepEqual(actual, expected);
        });
    });

    describe('getTestSpec', () => {
        it('should return test spec', async () => {
            const ast = parser.parser(raw);

            const actual = parser.getTestSpec({
                titlePath: ['Feature-name', 'Describe 1', 'It 1'],
                fullTitle: () => 'Feature-name Describe 1 It 1',
                files: { hermione: { ast, filePath } },
            } as unknown as Test);

            chai.assert.equal(_.get(actual, '0.name', ''), 'beforeEach');
            chai.assert.lengthOf(_.get(actual, '0.steps', []), 1);
        });
    });

    afterEach(() => {
        sinon.restore();
    });
});
