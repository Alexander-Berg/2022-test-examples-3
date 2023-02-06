import { promises as fs } from 'fs';
import _ from 'lodash';
import yaml from 'js-yaml';

import createDebug from 'debug';

import { Tide, TestFile, TestFileParser } from '../../..';
import { normalizeTestSpec } from './data-helpers';
import { normalizeRaw, prenormalizeRaw } from './raw-helpers';

import { TOOL, FILE_EXTS, SPECS_TYPE_KEYS, TITLE_KEYS, IGNORE_KEYS, HOOK_KEYS } from '../constants';
import {
    FileParserLike,
    FileParserOptions,
    Test,
    TestFileData,
    TestSpec,
    TestSpecRaw,
} from '../../../types';

const debug = createDebug('tide-testpalm-parser');

const defaultOptions: FileParserOptions = {
    enabled: true,
    comments: true,
    silent: false,
    verbose: false,
    parse: true,
};

export default class TestpalmParser extends TestFileParser {
    constructor({ options = {} }: Partial<FileParserLike> = {}) {
        super({
            tool: TOOL,
            fileExts: FILE_EXTS,
            parser: yaml,
            options: _.defaultsDeep(options, defaultOptions),
        });
    }

    async read(tide: Tide, filePath: string): Promise<void> {
        if (!this.options.enabled) {
            return;
        }

        debug(filePath);

        if (this.options.verbose && !this.options.silent) {
            console.log(`- ${filePath}`);
        }

        const raw = await fs.readFile(filePath, 'utf8');

        let data: Record<string, any> = {};

        if (this.options.parse) {
            try {
                // TODO: разобраться с алиасами, js-yaml их теряет
                const refs = _.uniq(
                    Array.from(raw.matchAll(/[^\S]\*(\S*)/gm)).map((arr) => (arr as string[])[1]),
                );

                // TODO: избавиться от ретраев
                const tryParse = (value: string, fixIndent?: boolean, limit = 1): void => {
                    try {
                        data = yaml.load(prenormalizeRaw(value, fixIndent as boolean)) || {};

                        if (refs.length) {
                            data['%%REFS%%'] = refs;
                        }
                    } catch (error) {
                        if (limit) {
                            tryParse(value, true, limit - 1);
                        } else if (!this.options.silent) {
                            console.log(`ParsingError in ${filePath}`);

                            if (this.options.verbose) {
                                console.error(error);
                            } else {
                                console.error(error.message);
                            }
                        }
                    }
                };

                tryParse(raw);
            } catch (error) {
                if (!this.options.silent) {
                    console.log(`Error in ${filePath}`);

                    if (this.options.verbose) {
                        console.error(error);
                    } else {
                        console.error(error.message);
                    }
                }
                return;
            }

            if (data.files && data.files.length) {
                data.files
                    // filter for service fields
                    .filter((filePath: string) => !filePath.startsWith('%%'))
                    .forEach((hermioneFilePath: string) => {
                        tide.fileCollection.addMapping(hermioneFilePath, filePath);
                    });
            }
        }

        const fileExt = this.getTestFileExt(filePath);
        const testFile = new TestFile({ tool: TOOL, filePath, fileExt, data, raw });

        debug(testFile);

        if (this.options.parse) {
            _.values(tide.constants.TYPES).forEach((type) => {
                const titlePaths = this.getTitlePaths(testFile, type);

                titlePaths.forEach((titlePath) => {
                    const test = tide.testCollection.addTest({
                        tool: TOOL,
                        type,
                        titlePath,
                        filePath,
                        files: testFile,
                    });

                    if (!test) {
                        return;
                    }

                    debug(test);

                    testFile.addTest(test);
                });
            });
        }

        tide.fileCollection.addFile(testFile);
    }

    async serialize(testFile: TestFile): Promise<string | NodeJS.ArrayBufferView> {
        const refs = testFile.data?.['%%REFS%%'] || [];

        delete testFile.data?.['%%REFS%%'];

        return normalizeRaw(yaml.dump(testFile.data, { lineWidth: 9999 }), { refs });
    }

    getTitlePaths(testFile: TestFile, type: string): (string | Record<string, any>)[][] {
        const titlePaths: (string | Record<string, any>)[][] = [];

        if (!testFile.data) {
            return titlePaths;
        }

        const testSpecsType = SPECS_TYPE_KEYS[type];
        const testSpecsTitle = _.pick(testFile.data, TITLE_KEYS);
        const testSpecs = _.get(testFile.data, testSpecsType);

        if (!testSpecs) {
            return [];
        }

        const traverse = (
            data: TestFileData,
            paths: (string | Record<string, any>)[] = [],
        ): void => {
            if (_.isObject(data) && !_.isArray(data)) {
                _.keys(data).map((key) => {
                    if (!IGNORE_KEYS.includes(key)) {
                        traverse(data[key], [...paths, key]);
                    }
                });
            } else {
                titlePaths.push(paths);
            }
        };

        traverse(testSpecs, [testSpecsTitle]);

        return titlePaths;
    }

    getTestSpec(test: Test): TestSpec {
        let testSpec: TestSpec = [];
        const testSpecKey = SPECS_TYPE_KEYS[test.type];

        [testSpecKey, ...test.titlePath.slice(1)].reduce((dataPart, titlePart) => {
            const testSpecDataPart = dataPart[titlePart];

            if (_.isArray(testSpecDataPart)) {
                testSpec.push({ name: 'it', steps: testSpecDataPart });
            }

            HOOK_KEYS.forEach((hook) => {
                if (_.get(testSpecDataPart, hook)) {
                    const item = { name: hook, steps: testSpecDataPart[hook] };

                    if (hook.startsWith('before')) {
                        testSpec.unshift(item);
                    } else {
                        testSpec.push(item);
                    }
                }
            });

            return testSpecDataPart;
        }, (test.files.testpalm as TestFile).data);

        return normalizeTestSpec(testSpec);
    }

    getTestSpecRaw(testSpec: TestSpec): TestSpecRaw {
        let raw = '';

        const steps = testSpec.flatMap((item) => item.steps);

        try {
            raw = yaml.dump(steps, { lineWidth: 9999 });
        } catch (error) {
            console.error(error);
        }

        return raw;
    }
}
