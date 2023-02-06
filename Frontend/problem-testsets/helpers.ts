import { IProblemTestset } from 'common/types/problem-test';
import { generateTestsetMapKey } from 'common/utils/helpers/problem-testsets';

export const getTestsetsList = (testsetsFullData: IProblemTestset[]) =>
    sortTestsets(testsetsFullData).map(({ id, name, sample }: IProblemTestset) => ({
        id,
        name,
        sample,
    }));

const sortTestsets = (source: IProblemTestset[]) =>
    source.sort(
        (firstTestset: IProblemTestset, secondTestset: IProblemTestset) =>
            Number(firstTestset.id) - Number(secondTestset.id),
    );

export const getTestsetMap = (testsetsFullData: IProblemTestset[], problemId: string) => {
    return testsetsFullData.reduce((accumulator, currentTestset) => {
        const { id: testsetId } = currentTestset;
        const testsetMapKey = generateTestsetMapKey(problemId, testsetId);

        return {
            ...accumulator,
            [testsetMapKey]: {
                data: currentTestset,
                fetchTestsStarted: false,
                fetchTestsError: null,
                updateTestsStarted: false,
                updateTestsError: null,
            },
        };
    }, {});
};
