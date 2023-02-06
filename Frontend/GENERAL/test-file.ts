import path from 'path';
import _ from 'lodash';
import { Collection } from 'jscodeshift';

import File from './file';
import Test from './test';
import { TestFileData, TestFileLike } from './types';

interface TestFileOriginal {
    filePath?: string;
    raw?: string | Buffer;
    data?: Record<string, any>;
}

export default class TestFile extends File {
    type: string | undefined;
    data: TestFileData | undefined;
    ast?: Collection;
    tests: Test[];

    protected _original: TestFileOriginal;
    get original(): TestFileOriginal {
        return this._original;
    }

    constructor({ tool, type, filePath, fileExt, raw, data, ast }: TestFileLike) {
        const relativeFilePath = filePath && path.relative(process.cwd(), filePath);

        super({ tool, filePath, fileExt });

        this._original = {
            filePath: relativeFilePath,
            raw: _.cloneDeep(raw),
            data: _.cloneDeep(data),
        };
        this.type = type;
        this.data = data;
        this.ast = ast;
        this.tests = [];
    }

    addTest(test: Test): void {
        this.tests.push(test);
    }
}
