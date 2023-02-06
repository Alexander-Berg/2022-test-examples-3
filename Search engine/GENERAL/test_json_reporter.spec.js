/*
    Test for jest json_reporter written on jest
 */
const path = require('path');
const runSync = require('child_process').execSync;
const reporterConf = require('../reporter_config.json')

const jestConfigPath = path.join(__dirname, 'jest_config.json');
const specsFolderPath = path.join(__dirname, 'fixture_suites');
const jestPath = path.join(__dirname, '..',  'node_modules', 'jest', 'bin', 'jest.js')

describe("Testing priemka jest json reporter", () => {

    const subprocResult = (specName) => {
        const specPath = path.join(specsFolderPath, specName);
        const results = runSync('node ' + jestPath + ' ' + specPath + ' --config=' + jestConfigPath);
        return results.toString().split("\n").filter(el => el.length);
    };

    const resultPrefix = reporterConf["logPrefix"]["result"];

    const extractResult = rawLogResults => {
        let result = rawLogResults.filter(el => el.startsWith(resultPrefix))[0];
        result = result.replace(resultPrefix, "");
        return JSON.parse(result);
    };


    test("Run spec not containing any errors", () => {
        const result = extractResult(subprocResult("suite_one.spec.js"));
        expect(result.hasErrors).toBe(false);
        expect(result.result.passed).toBe(3);
        const resKeys = Object.keys(result.result.results);
        expect(resKeys).toContain("Suite one");
        expect(resKeys).toContain("Suite two");
    });

    test("Run spec containing assertion errors", () => {
        try {
            extractResult(subprocResult("suite_two.spec.js"));
        }
        catch (e) {
            const res = extractResult(e.stdout.toString().split("\n"));
            expect(res.result.passed).toBe(1);
            expect(res.result.failed).toBe(2);
            expect(res.hasErrors).toBe(true);
            expect(res.result.failureMessages.length > 0).toBe(true);
        }

    });

    //TODO: custom config and tests for early exit from reporter (capture out and catch RuntimeErrors)

});