import { assert } from 'chai';
import { TestdataParserConfig } from '../../../src/parsers/tide-testdata-parser/types';
import { parseConfig } from '../../../src/parsers/tide-testdata-parser/config';
import { Test, TestdataFile } from '../../../src';

describe('tide-testdata-parser / config', () => {
    describe('default config', () => {
        let config: TestdataParserConfig;

        before(() => {
            config = parseConfig({});
        });

        it('should return correct test-data directory for hermione file', () => {
            const expected = '/some/path/test-data';
            const actual = config.baseDirPath('/some/path/some-test.hermione.js');

            assert.equal(actual, expected);
        });

        it('should return correct test-data directory for directory', () => {
            const expected = '/some/path/';
            const actual = config.baseDirPath('/some/path/');

            assert.equal(actual, expected);
        });

        it('should return correct hash directory for test', () => {
            const expected = '/some/path/test-data/c2d720a';
            const actual = config.dirPath(
                { fullTitle: () => 'Some Test Title' } as Test,
                '/some/path/test-data/',
            );

            assert.equal(actual, expected);
        });

        it('should return correct file path for testdata file', () => {
            const expected = '/some/path/test-data/c2d720a/some-browser/228458095a950207.json.gz';
            const actual = config.filePath(
                {
                    data: { key: 'value' },
                    fileExt: 'json.gz',
                    filePath: '/some/path/test-data/abcdefg/some-browser/test-data.json.gz',
                } as unknown as TestdataFile,
                '/some/path/test-data/c2d720a',
            );

            assert.equal(actual, expected);
        });

        it('should return file path unchanged if generation of previous segments was failed', () => {
            const expected = '/some/path/test-data/abcdefg/some-browser/test-data.json.gz';
            const actual = config.filePath(
                {
                    data: { key: 'value' },
                    fileExt: 'json.gz',
                    filePath: '/some/path/test-data/abcdefg/some-browser/test-data.json.gz',
                } as unknown as TestdataFile,
                undefined,
            );

            assert.equal(actual, expected);
        });
    });
});
