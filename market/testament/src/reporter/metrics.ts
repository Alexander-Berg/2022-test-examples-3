import fs from 'fs';
import path from 'path';
import crypto from 'crypto';

import {GlobalConfig} from '@jest/types/build/Config';
import {AggregatedResult, Status} from '@jest/test-result';
import {Context, Reporter} from '@jest/reporters';

export type ReportItem = {
    timestamp: number;
    status: Status;
    duration: number;
    testId: string;
    testName: string;
};

export type Options = {
    saveTo?: string;
};

export default class MyCustomReporter implements Partial<Reporter> {
    constructor(globalConfig: GlobalConfig, private options: Options) {}

    onRunComplete(
        contexts: Set<Context>,
        results: AggregatedResult,
    ): Promise<void> | void {
        if (!this.options.saveTo) {
            throw new Error('Reporter option saveTo does not specified');
        }

        const reportItems: ReportItem[] = [];

        for (const testResult of results.testResults) {
            for (const item of testResult.testResults) {
                const testId = crypto
                    .createHash('md4')
                    .update(`${testResult.testFilePath}:${item.fullName}`)
                    .digest('hex')
                    .slice(0, 7);
                const status: Status =
                    item.status === 'pending' ? 'skipped' : item.status;
                reportItems.push({
                    testId,
                    testName: item.fullName,
                    duration: item.duration ?? 0,
                    timestamp: testResult.perfStats.start,
                    status,
                });
            }
        }

        const absPath = path.resolve(this.options.saveTo);

        fs.mkdirSync(path.dirname(absPath), {recursive: true});
        fs.writeFileSync(absPath, JSON.stringify(reportItems));
    }
}
