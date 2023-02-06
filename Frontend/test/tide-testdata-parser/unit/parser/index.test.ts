import { assert } from 'chai';
import _ from 'lodash';
import TestdataParser from '../../../../src/parsers/tide-testdata-parser/parser';
import { parseConfig } from '../../../../src/parsers/tide-testdata-parser/config';
import { TestdataFile } from '../../../../src';

function mkTestdataFile(): TestdataFile {
    return {
        data: { key: 'value' },
        fileExt: 'json.gz',
        filePath: '/some/path/test-data/abcdefg/some-browser/some-hash.json.gz',
        tests: [
            {
                fullTitle: (): string => 'Some Test Title',
                files: {
                    hermione: {
                        filePath: '/some/path/some-test.hermione.js',
                    },
                },
            },
        ],
    } as unknown as TestdataFile;
}

describe('tide-testdata-parser/parser/index', () => {
    describe('getFilePath', () => {
        let parser: TestdataParser;

        beforeEach(() => {
            const config = parseConfig({});
            parser = new TestdataParser({}, config);
        });

        it('should generate correct file path given testdata file', () => {
            const expected = '/some/path/test-data/c2d720a/some-browser/228458095a950207.json.gz';

            const actual = parser.getFilePath(mkTestdataFile());

            assert.equal(actual, expected);
        });

        it('should preserve original file path if testdata file is not linked to tests', () => {
            const expected = '/original/testdata/abcdefg/some-browser/some-hash.json.gz';

            const file = mkTestdataFile();
            file.filePath = undefined;
            _.set(file, 'original', {});
            file.original.filePath = '/original/testdata/abcdefg/some-browser/some-hash.json.gz';
            file.tests = [];

            const actual = parser.getFilePath(file);

            assert.equal(actual, expected);
        });
    });
});
