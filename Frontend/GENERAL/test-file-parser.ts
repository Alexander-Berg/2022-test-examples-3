import _ from 'lodash';

import { Test, TestFile, TestSpec, TestSpecRaw } from './types';
import FileParser from './file-parser';
import { TITLE_KEYS } from './parsers/tide-hermione-parser/constants';

export default abstract class TestFileParser extends FileParser {
    protected constructor({ tool, fileExts, parser, options = {} }) {
        super({ tool, fileExts, parser, options });
    }

    static getFullTitle(parts: (string | Record<string, any>)[]): string {
        return parts.map((part) => TestFileParser.getTitleFromObject(part)).join(' ');
    }

    static getStringTitleParts(parts: (string | Record<string, any>)[]): string[] {
        return parts.map((part) => TestFileParser.getTitleFromObject(part));
    }

    static getTitleFromObject(
        obj: Record<string, string> | string,
        titleKeys: string[] = TITLE_KEYS,
    ): string {
        return typeof obj === 'string'
            ? obj
            : titleKeys
                  .reduce((result, key) => (obj[key] ? result + obj[key] + ' / ' : result), '')
                  .slice(0, -3);
    }

    abstract getTitlePaths(testFile: TestFile, type?: string): (string | Record<string, any>)[][];

    abstract getTestSpec(test: Test): TestSpec;

    abstract getTestSpecRaw(testSpec: TestSpec, options: Record<string, any>): TestSpecRaw;

    getTestFileExt(filePath: string): string {
        return this.fileExts.find((fileExt) => filePath.endsWith(fileExt)) as string;
    }

    isModified(file: TestFile, modifiedRaw?: string | NodeJS.ArrayBufferView): boolean {
        if (file.ast && modifiedRaw) {
            return !_.isEqual(file.original.raw, modifiedRaw);
        }

        if (file.original.data || file.data) {
            return !_.isEqual(file.original.data, file.data);
        }

        if (file.original.raw || file.raw || modifiedRaw) {
            if (modifiedRaw) {
                return !_.isEqual(file.original.raw, modifiedRaw);
            }

            return !_.isEqual(file.original.raw, file.raw);
        }

        return false;
    }
}
