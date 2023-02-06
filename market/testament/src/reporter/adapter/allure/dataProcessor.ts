import {RootData, GroupData, StepData} from '../../generator';

export type TestData = {
    fullName: string;
    steps: StepData[];
};

export function parseTestsData(data?: RootData): TestData[] {
    // eslint-disable-next-line
    const tests: TestData[] = [];

    function findTests(
        group: GroupData,
        beforeAllSteps: StepData[] = [],
        afterAllSteps: StepData[] = [],
        parentGroupsName: string[] = [],
    ) {
        parentGroupsName.push(group.name);

        group.beforeAll.forEach(hook => {
            // eslint-disable-next-line
            beforeAllSteps = beforeAllSteps.concat(hook.steps);
        });

        group.afterAll.forEach(hook => {
            // eslint-disable-next-line
            afterAllSteps = afterAllSteps.concat(hook.steps);
        });

        group.tests.forEach(test => {
            tests.push({
                fullName: `${parentGroupsName.join(' ')} ${test.name}`,
                steps: [...beforeAllSteps, ...test.steps, ...afterAllSteps],
            });
        });

        group.groups.forEach(item => findTests(item));
    }

    data?.groups.forEach(group => findTests(group, [], []));

    return tests;
}
