import { promises as fs } from 'fs';
import { gunzip as gunzipCallback, gzip as gzipCallback } from 'zlib';
import { promisify } from 'util';
import path from 'path';
import _ from 'lodash';
import createDebug from 'debug';

import { FileParser, Tide } from '../../..';
import { FILE_EXTS, TOOL } from '../constants';
import TestdataFile from '../testdata-file';
import { FileParserLike, FileParserOptions } from '../../../types';
import { TestdataParserConfig } from '../types';

const gzip = promisify(gzipCallback);
const gunzip = promisify(gunzipCallback);

const debug = createDebug('tide-testdata-parser');

const defaultOptions: FileParserOptions = {
    enabled: true,
    silent: false,
    verbose: false,
    parse: true,
};

export default class TestdataParser extends FileParser {
    private _pluginConfig: TestdataParserConfig;

    constructor(
        { options = {} }: Partial<FileParserLike> = {},
        pluginConfig: TestdataParserConfig,
    ) {
        super({
            tool: TOOL,
            fileExts: FILE_EXTS,
            parser: JSON,
            options: _.defaultsDeep(options, defaultOptions),
        });

        this._pluginConfig = pluginConfig;
    }

    async read(tide: Tide, filePath: string): Promise<void> {
        if (!this.options.enabled) {
            return;
        }

        debug(filePath);

        const raw = await fs.readFile(filePath);

        let contents: string | undefined;

        if (this.options.parse) {
            contents = (await gunzip(raw)).toString();
        }

        const testdataFile = new TestdataFile({
            tool: TOOL,
            filePath,
            fileExt: FILE_EXTS[0],
            raw,
            contents,
        });

        tide.fileCollection.addFile(testdataFile);

        debug(testdataFile);
    }

    async serialize(file: TestdataFile): Promise<Buffer> {
        return file.contents ? await gzip(file.contents) : file.raw;
    }

    getFilePath(file: TestdataFile): string | undefined {
        const baseDirPath = this._pluginConfig.baseDirPath(
            _.get(file, 'tests.0.files.hermione.filePath'),
        );
        const dirPath = baseDirPath && this._pluginConfig.dirPath(file.tests[0], baseDirPath);
        let filePath: string | undefined;

        if (file.isRenamed === false) {
            const originalFilePath = file.original.filePath;
            if (originalFilePath) {
                const browser = path.basename(path.dirname(originalFilePath));
                filePath = dirPath && path.join(dirPath, browser, path.basename(originalFilePath));
            }
        } else {
            filePath = dirPath && this._pluginConfig.filePath(file, dirPath);
        }
        return filePath ?? file.original.filePath;
    }

    isModified(file: TestdataFile, modifiedRaw?: string | NodeJS.ArrayBufferView): boolean {
        if (file.original.contents || file.contents) {
            return file.original.contents !== file.contents;
        }

        if (modifiedRaw) {
            return !_.isEqual(file.original.raw, modifiedRaw);
        }

        return !_.isEqual(file.original.raw, file.raw);
    }
}
