import _ from 'lodash';

import Test from './test';
import { PLATFORMS } from './constants';
import { GroupedTests, BaseTestLike } from './types';
import { TestFileParser } from './index';

export default class TestCollection {
    private _tools: Set<string>;
    private _fileExts: Record<string, string[]>;
    private _original: { tests: Record<string, Test> };
    private _tests: Record<string, Test>;
    private _testsByFilePath: GroupedTests;
    private _filter: (test: Test) => boolean;

    constructor(tests: Record<string, Test> = {}) {
        this._tools = new Set();
        this._fileExts = {};
        this._original = { tests };
        this._tests = _.mapValues(tests, _.clone);
        this._testsByFilePath = {};
        this._filter = (): boolean => true;

        if (!_.isEmpty(tests)) {
            _.values(tests).forEach((test) => {
                _.values(test.filePaths).forEach((filePath) => {
                    _.defaults(this._testsByFilePath, { [filePath]: [] });

                    this._testsByFilePath[filePath].push(test);
                });
            });
        }
    }

    getFilter(): (test: Test) => boolean {
        return this._filter;
    }

    setFilter(filter: (test: Test) => boolean): void {
        this._filter = filter;
    }

    addTool({ tool, fileExts }: { tool: string; fileExts: string[] }): void {
        this._tools.add(tool);
        this._fileExts[tool] = fileExts;
    }

    addTest(params: BaseTestLike): Test | undefined {
        const { type, titlePath, filePath } = params;
        const baseFilePath = this.getTestBaseFilePath(filePath);
        const testId = this._getTestId(titlePath, baseFilePath, type);
        let test = this.getTest(testId);

        _.defaults(this._testsByFilePath, { [filePath]: [] });

        if (test) {
            test.update(params);
        } else {
            const platform = this._getTestPlatform(baseFilePath);

            test = new Test({ ...params, id: testId, baseFilePath, platform: platform as string });
            if (!this._filter(test)) {
                return;
            }

            this._tests[testId] = test;
            this._testsByFilePath[filePath].push(test);
        }

        return test;
    }

    getTest(testId: string): Test;
    getTest(params: {
        titlePath: (string | Record<string, any>)[];
        filePath: string;
        type: string;
    }): Test;
    getTest(
        testIdOrParams:
            | string
            | { titlePath: (string | Record<string, any>)[]; filePath: string; type: string },
    ): Test {
        let testId;

        if (typeof testIdOrParams === 'string') {
            testId = testIdOrParams;
        } else {
            const params = testIdOrParams;
            const baseFilePath = this.getTestBaseFilePath(params.filePath);

            testId = this._getTestId(params.titlePath, baseFilePath, params.type);
        }

        return this._tests[testId];
    }

    getTests(): Test[] {
        return _.values(this._tests);
    }

    eachTest(cb: (test: Test) => void): void {
        _.values(this._tests).forEach(cb);
    }

    mapTests<T>(cb: (test: Test) => T): T[] {
        return _.values(this._tests).map(cb);
    }

    filterTests(cb: (test: Test) => boolean): void {
        this._tests = _.keys(this._tests).reduce((acc, testId) => {
            const test = this._tests[testId];

            if (cb(test)) {
                acc[testId] = test;
            }

            return acc;
        }, {});
    }

    groupTests(cb: (test: Test) => string[] | string[][]): GroupedTests {
        const groupedTests = {};

        this.eachTest((test) => {
            let groupPaths = cb(test);

            if (!groupPaths) {
                return;
            }

            if (!_.isArray(groupPaths[0])) {
                groupPaths = [groupPaths as string[]];
            }

            groupPaths.forEach((groupPath) => {
                if (groupPath[groupPath.length - 1]) {
                    _.defaultsDeep(groupedTests, _.set({}, groupPath, []));
                } else {
                    _.defaultsDeep(groupedTests, _.set({}, _.initial(groupPath), {}));
                }

                const targetGroup = _.get(groupedTests, groupPath) as Test[];

                if (groupPath[groupPath.length - 1]) {
                    targetGroup.push(test);
                }
            });
        });

        return groupedTests;
    }

    getTestsByFilePath(filePath?: string): Test[] | Record<string, Test[]> {
        return filePath ? this._testsByFilePath[filePath] : this._testsByFilePath;
    }

    getTestBaseFilePath(filePath: string): string {
        const currentExt = _.flatten(_.values(this._fileExts)).find((ext) =>
            filePath.endsWith(ext),
        );

        return filePath.replace(new RegExp(`.${currentExt}$`), '');
    }

    private _getTestId(
        titlePath: (string | Record<string, any>)[],
        baseFilePath: string,
        type: string,
    ): string {
        return [TestFileParser.getFullTitle(titlePath), baseFilePath, type].join(',');
    }

    private _getTestPlatform(baseFilePath: string): string | undefined {
        return PLATFORMS.find((platform) => baseFilePath.includes(platform));
    }
}
