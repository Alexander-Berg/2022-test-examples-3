import _ from 'lodash';
import File from '../../file';
import Test from '../../test';
import { TestdataFileLike } from '../../types';

interface TestdataOriginal {
    filePath?: string;
    raw?: string | Buffer;
    contents?: string;
}

export default class TestdataFile extends File {
    raw: Buffer;
    // Содержимое дампа в виде строки. Используется при записи
    private _contents?: string;
    private _isContentsChanged: boolean;
    get contents(): string | undefined {
        return this._contents;
    }
    set contents(value: string | undefined) {
        this._contents = value;
        this._isContentsChanged = true;
    }

    // JSON объект строки из contents. Доступен только для чтения и не меняется при изменениях contents
    private _data: Record<string, any>;
    get data(): Record<string, any> | undefined {
        if (!this.contents) {
            return;
        }
        if (this._isContentsChanged) {
            this._data = JSON.parse(this.contents);
        }
        return this._data;
    }

    tests: Test[] = [];
    protected _original: TestdataOriginal;
    get original(): TestdataOriginal {
        return this._original;
    }

    constructor({ tool, filePath, fileExt, raw, contents }: TestdataFileLike) {
        super({ tool, filePath, fileExt, raw });

        this._isContentsChanged = true;
        this._contents = contents;
        this._data = {};
        this.raw = raw;

        this._original = {
            filePath,
            raw,
            contents,
        };
    }

    addTest(test: Test): void {
        this.tests.push(test);
    }
}
