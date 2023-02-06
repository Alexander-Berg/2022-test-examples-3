import path from 'path';

import {Config} from '@jest/types';
import {AggregatedResult, TestResult, AssertionResult} from '@jest/test-result';
import {Context, Reporter} from '@jest/reporters';
import {
    AllureRuntime,
    LabelName as AllureLabelName,
    TestResult as AllureTestResult,
    Status as AllureStatus,
} from 'allure2-js-commons';
import stripAnsi from 'strip-ansi';
import allure from 'allure-commandline';

import relativePath from '../../../utils/relativePath';
import ReportGenerator, {RootData, StepData} from '../../generator';
import {parseTestsData, TestData} from './dataProcessor';
import getAllureStatus from './getAllureStatus';

type Options = {
    saveTo: string;
};

export default class AllureReporter implements Partial<Reporter> {
    private allureResultDir: string;

    private saveTo: string;

    private allureRuntime: AllureRuntime;

    constructor(globalConfig: Config.GlobalConfig, options: Options) {
        this.allureResultDir = path.join(
            process.cwd(),
            'allure-results',
            process.pid.toString(),
        );
        this.saveTo = path.resolve(options.saveTo || 'html_reports/allure');

        this.allureRuntime = new AllureRuntime({
            resultsDir: this.allureResultDir,
        });
    }

    async onRunComplete(
        contexts: Set<Context>,
        results: AggregatedResult,
    ): Promise<void> {
        results.testResults.forEach(testResult => {
            const testData = ReportGenerator.readResultData(
                testResult.testFilePath,
            );

            if (testData) {
                this.processTestResult(testResult, testData);
            }
        });

        return new Promise((resolve, reject) => {
            allure([
                'generate',
                this.allureResultDir,
                '--clean',
                '-o',
                this.saveTo,
            ]).on('close', exitCode => {
                if (!exitCode) {
                    resolve();
                } else {
                    reject();
                }
            });
        });
    }

    private processTestResult(result: TestResult, data?: RootData): void {
        if (!result.testResults.length) {
            this.processBrokenTest(result);
        } else {
            this.processTests(result, data);
        }
    }

    private processBrokenTest(result: TestResult): void {
        const allureTestResult = new AllureTestResult();

        allureTestResult.name = relativePath(result.testFilePath);
        allureTestResult.fullName = result.testFilePath;
        allureTestResult.status = AllureStatus.BROKEN;
        allureTestResult.start = result.perfStats.start;
        allureTestResult.stop = result.perfStats.end;

        if (result.testExecError) {
            allureTestResult.statusDetails.message = stripAnsi(
                result.testExecError.message || '',
            );
            allureTestResult.statusDetails.trace = stripAnsi(
                result.testExecError.stack || '',
            );
        } else if (result.failureMessage) {
            allureTestResult.statusDetails.message = stripAnsi(
                result.failureMessage,
            );
        }

        this.allureRuntime.writeResult(allureTestResult);
    }

    private processTests(result: TestResult, rootData?: RootData) {
        const testsData = parseTestsData(rootData);

        result.testResults.forEach(testResult => {
            const data = testsData.find(
                testData => testData.fullName === testResult.fullName,
            );
            this.processTest(result, testResult, data);
        });
    }

    private processTest(
        result: TestResult,
        assertionResult: AssertionResult,
        testData?: TestData,
    ) {
        const allureTestResult = new AllureTestResult();

        allureTestResult.name = assertionResult.title;
        allureTestResult.fullName = assertionResult.fullName;
        allureTestResult.status = getAllureStatus(assertionResult.status);
        allureTestResult.start = result.perfStats.start;
        allureTestResult.stop =
            result.perfStats.start + (assertionResult.duration || 0);

        if (assertionResult.failureMessages.length) {
            allureTestResult.statusDetails.message = stripAnsi(
                assertionResult.failureMessages[0],
            );
        }

        if (assertionResult.failureDetails.length) {
            allureTestResult.statusDetails.trace = JSON.stringify(
                assertionResult.failureDetails[0],
                null,
                4,
            );
        }

        if (assertionResult.ancestorTitles.length > 2) {
            allureTestResult.labels.push({
                name: AllureLabelName.SUB_SUITE,
                value: assertionResult.ancestorTitles[2],
            });
        }

        if (assertionResult.ancestorTitles.length > 1) {
            allureTestResult.labels.push({
                name: AllureLabelName.SUITE,
                value: assertionResult.ancestorTitles[1],
            });
        }

        if (assertionResult.ancestorTitles.length > 0) {
            allureTestResult.labels.push({
                name: AllureLabelName.PARENT_SUITE,
                value: assertionResult.ancestorTitles[0],
            });
        }

        if (testData && testData.steps) {
            const formatStep = (stepData: StepData): any => ({
                name: stepData.name,
                status: getAllureStatus(stepData.status),
                steps: stepData.steps.map(formatStep),
            });

            allureTestResult.steps = testData.steps.map(formatStep);
        }

        this.allureRuntime.writeResult(allureTestResult);
    }
}
