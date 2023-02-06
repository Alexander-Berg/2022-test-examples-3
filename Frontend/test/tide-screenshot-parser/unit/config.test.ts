import { assert } from 'chai';
import { ScreenshotParserConfig } from '../../../src/parsers/tide-screenshot-parser/types';
import { parseConfig } from '../../../src/parsers/tide-screenshot-parser/config';
import { Test, ScreenshotFile } from '../../../src';

describe('tide-screenshot-parser/config', () => {
    describe('default config', () => {
        let config: ScreenshotParserConfig;

        before(() => {
            config = parseConfig({});
        });

        it('should return correct test-data directory for hermione file', () => {
            const expected = '/some/path/screens';
            const actual = config.baseDirPath('/some/path/some-test.hermione.js');

            assert.equal(actual, expected);
        });

        it('should return correct test-data directory for directory', () => {
            const expected = '/some/path/';
            const actual = config.baseDirPath('/some/path/');

            assert.equal(actual, expected);
        });

        it('should return correct hash directory for test', () => {
            const expected = '/some/path/screens/c2d720a';
            const actual = config.dirPath(
                { fullTitle: () => 'Some Test Title' } as Test,
                '/some/path/screens',
            );

            assert.equal(actual, expected);
        });

        it('should return correct file path for screenshot file', () => {
            const expected = '/some/path/screens/c2d720a/some-browser/screenshot.png';
            const actual = config.filePath(
                {
                    data: { key: 'value' },
                    fileExt: 'png',
                    filePath: '/some/path/abcdefg/some-browser/screenshot.png',
                } as unknown as ScreenshotFile,
                '/some/path/screens/c2d720a',
            );

            assert.equal(actual, expected);
        });

        it('should return file path unchanged if generation of previous segments was failed', () => {
            const expected = '/some/path/screens/abcdefg/some-browser/screenshot.png';
            const actual = config.filePath(
                {
                    data: { key: 'value' },
                    fileExt: 'png',
                    filePath: '/some/path/screens/abcdefg/some-browser/screenshot.png',
                } as unknown as ScreenshotFile,
                undefined,
            );

            assert.equal(actual, expected);
        });
    });
});
