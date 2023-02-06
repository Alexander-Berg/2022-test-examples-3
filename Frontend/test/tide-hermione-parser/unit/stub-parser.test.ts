import fs from 'fs';
import chai from 'chai';
import sinon, { assert } from 'sinon';
import _ from 'lodash';

import HermioneStubParser from '../../../src/parsers/tide-hermione-stub-parser/parser';
import { Tide, TestFile, Test } from '../../../src';

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
const data = {
    feature: 'Feature-name',
    specs: {
        'Describe 1': {
            beforeEach: [
                { name: 'yaOpenSerp', arguments: [{ text: 'text' }, undefined], object: 'browser' },
            ],
            'It 1': [
                { name: 'assertView', arguments: ['plain', undefined], object: 'browser' },
                { name: 'assertView', arguments: ['plain 2', undefined], object: 'browser' },
            ],
            'It 2': [
                { name: 'assertView', arguments: ['plain', undefined], object: 'browser' },
                { name: 'assertView', arguments: ['plain 2', undefined], object: 'browser' },
            ],
        },
        'It 3': [
            { name: 'yaOpenSerp', arguments: [{ text: 'text' }, undefined], object: 'browser' },
            { name: 'assertView', arguments: ['plain', undefined], object: 'browser' },
        ],
    },
    files: ['some-filepath'],
};
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

describe('tide-hermione-parser / stub-parser', () => {
    let tide: Tide;
    let parser: HermioneStubParser;
    const filePath = 'some-filepath';

    beforeEach(() => {
        tide = mkTideStub();

        parser = new HermioneStubParser();

        sinon.stub(fs.promises, 'readFile').withArgs(filePath).resolves(raw);
    });

    describe('read', () => {
        beforeEach(() => {
            sinon.stub(parser.parser, 'parse').returns({});
        });

        it('should read and parse hermione file', async () => {
            sinon.stub(parser, 'getTitlePaths').returns([]);

            await parser.read(tide, filePath);

            assert.calledOnce(fs.promises.readFile as any);
            assert.calledOnce(parser.parser.parse);
        });

        it('should add file to fileCollection', async () => {
            sinon.stub(parser, 'getTitlePaths').returns([]);

            await parser.read(tide, filePath);

            assert.calledOnce(tide.fileCollection.addFile as any);
        });

        it('should add tests to testCollection', async () => {
            sinon.stub(parser, 'getTitlePaths').returns([['test 1'], ['test 2']]);

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

            sinon.stub(parser.parser, 'parse').returns(data);

            const actual = parser.getTitlePaths({ data } as unknown as TestFile);

            chai.assert.deepEqual(actual, expected);
        });
    });

    describe('getTestSpec', () => {
        it('should return test spec', async () => {
            sinon.stub(parser.parser, 'parse').returns(data);

            const actual = parser.getTestSpec({
                titlePath: ['Feature-name', 'Describe 1', 'It 1'],
                files: { hermione: { data } },
            } as unknown as Test);

            chai.assert.equal(_.get(actual, '0.name', ''), 'beforeEach');
            chai.assert.lengthOf(_.get(actual, '0.steps', []), 1);
        });
    });

    afterEach(() => {
        sinon.restore();
    });
});
