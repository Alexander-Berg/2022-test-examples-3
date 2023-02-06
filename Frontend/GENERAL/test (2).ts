import _ from 'lodash';
import { TestLike } from './types';
import File from './file';
import { TestFileParser } from './index';

interface TestOriginal {
    titlePath: (string | Record<string, any>)[];
}

export default class Test {
    private _original: TestOriginal;

    id: string;
    baseFilePath: string;
    tools: Set<string>;
    filePaths: Record<string, string>;
    files: Record<string, File | File[]>;
    type: string;
    platform: string;
    titlePath: (string | Record<string, string>)[];

    constructor({ id, baseFilePath, tool, type, platform, titlePath, filePath, files }: TestLike) {
        this._original = {
            titlePath: _.cloneDeep(titlePath),
        };

        this.id = id;
        this.baseFilePath = baseFilePath;
        this.tools = new Set();
        this.filePaths = {};
        this.files = {};
        this.type = type;
        this.platform = platform;
        this.titlePath = titlePath;

        this.update({ tool, filePath, files });
    }

    get original(): TestOriginal {
        return this._original;
    }

    update({
        tool,
        filePath,
        files,
    }: {
        tool: string;
        filePath: string;
        files: File | File[];
    }): void {
        this.tools.add(tool);
        this.filePaths[tool] = filePath;
        this.files[tool] = files;
    }

    fullTitle(): string {
        return TestFileParser.getFullTitle(this.titlePath);
    }
}
