import _ from 'lodash';

import { Tide, Test, TestFileParser } from '../../../types';
import { HunterConfig, GroupedTestSpecs, GroupedTestSpecRaws } from '../types';

export class TestSpecCollection {
    private _testSpecs: { [testId: string]: GroupedTestSpecs };
    private _testSpecRaws: { [testId: string]: GroupedTestSpecRaws };
    private _parsers: { [tool: string]: TestFileParser };
    private _pluginConfig: HunterConfig;

    constructor(testSpecs = {}, tide: Tide, pluginConfig: HunterConfig) {
        this._testSpecs = testSpecs;
        this._testSpecRaws = {};
        this._parsers = _.pick(tide.parsers, ['hermione', 'testpalm']) as Record<
            string,
            TestFileParser
        >;
        this._pluginConfig = pluginConfig;
    }

    addTestSpec(test: Test): void {
        const comparisonTools = this._pluginConfig.tool ? [this._pluginConfig.tool] : test.tools;

        comparisonTools.forEach((tool: string) => {
            _.defaults(this._testSpecs, { [test.id]: {} });
            _.defaults(this._testSpecRaws, { [test.id]: {} });

            const testSpec = this._parsers[tool].getTestSpec(test);
            const testSpecRaw = this._parsers[tool].getTestSpecRaw(testSpec, {});

            this._testSpecs[test.id][tool] = testSpec;
            this._testSpecRaws[test.id][tool] = testSpecRaw;
        });
    }

    getTestSpec(test: Test): GroupedTestSpecs {
        return this._testSpecs[test.id];
    }

    getTestSpecRaw(test: Test): GroupedTestSpecRaws {
        return this._testSpecRaws[test.id];
    }

    get testSpecs(): { [testId: string]: GroupedTestSpecs } {
        return this._testSpecs;
    }
}
