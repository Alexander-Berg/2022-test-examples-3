/* eslint-disable @typescript-eslint/no-non-null-assertion */

import { IProblemTestset } from 'common/types/problem-test';

import { storeWithValidationResults } from 'client/components/problem-validators/__stories__/constants';
import { getTestsetsList, getTestsetMap } from 'client/store/problem-testsets/helpers';
import { RootState } from 'client/store/types';

export const problemId = '1162658/2019_07_11/JQIc0JGCTG';

const testsetsData = [
    {
        id: 0,
        outputFilePattern: 'tests/%s.a',
        sample: true,
        inputFilePattern: 'tests/{001-004}',
        name: 'samples',
        tests: [
            {
                inputPath: 'tests/001',
                outputPath: 'tests/001.a',
            },
            {
                inputPath: 'tests/002',
                outputPath: 'tests/002.a',
            },
            {
                inputPath: 'tests/003',
                outputPath: 'tests/003.a',
            },
            {
                inputPath: 'tests/004',
                outputPath: 'tests/004.a',
            },
        ],
    },
    {
        id: 1,
        outputFilePattern: 'tests/%s.a',
        sample: true,
        inputFilePattern: 'tests/{001-002}',
        name: '1',
        tests: [
            {
                inputPath: 'tests/001',
                outputPath: 'tests/001.a',
            },
            {
                inputPath: 'tests/002',
                outputPath: 'tests/002.a',
            },
        ],
    },
] as IProblemTestset[];

export const store: Partial<RootState> = {
    problemTestsets: {
        [problemId]: {
            fetchStarted: false,
            fetchError: null,
            removeStarted: false,
            removeError: null,
            addTestsetStarted: false,
            addTestsetError: null,
            addSampleStarted: false,
            addSampleError: null,
            testsets: getTestsetsList(testsetsData),
            testsetsMap: getTestsetMap(testsetsData, problemId),
        },
    },
};

export const storeWithValidity: Partial<RootState> = {
    ...store,
    problemSettings: {
        ...storeWithValidationResults.problemSettings!,
    },
};

export const storeWithAddTestsetStarted: Partial<RootState> = {
    ...store,
    problemTestsets: {
        [problemId]: {
            ...store.problemTestsets![problemId],
            addTestsetStarted: true,
        },
    },
};
