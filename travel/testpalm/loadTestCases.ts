import chalk from 'chalk';

import {TRAVEL_TESTPALM_PROJECT_ID} from './utilities/constants';

import {ITestCase, ITestDefinition} from './utilities/types';

import {TestCaseWriter} from './utilities/TestCaseWriter';
import {TestPalmFetcher} from './utilities/TestPalmFetcher';
import {TestPalmFilterParser} from './utilities/TestPalmFilterParser';

import {ITestpalmToolArgs} from './index';

export async function loadTestCases({
    auth,
    filter,
    dir,
}: ITestpalmToolArgs): Promise<void> {
    const fetcher = new TestPalmFetcher(auth);

    const definitions = await fetcher.fetch<ITestDefinition[]>(
        `definition/${TRAVEL_TESTPALM_PROJECT_ID}`,
    );

    const filterParser = new TestPalmFilterParser(definitions);

    const expression = filterParser.parse(filter);

    const testCases = await fetcher.fetch<ITestCase[]>(
        `testcases/${TRAVEL_TESTPALM_PROJECT_ID}`,
        {expression: JSON.stringify(expression)},
    );

    const writer = new TestCaseWriter(dir, definitions);

    const writeResult = await writer.write(testCases);

    console.log(chalk.green(`Сохранено ямлов: ${writeResult.created}`));
}
