import isUndefined from 'lodash/isUndefined';

import { IProblem } from 'common/types/problem';

import { selectValidationResultsTests } from 'client/selectors/problem-validators';
import { RootState } from 'client/store/types';
import { IValidatedTest } from 'common/types/problem-validator';

export const selectFetchStarted = (state: RootState, problemId: IProblem['id']) =>
    (state.problemTestsets[problemId] || {}).fetchStarted;

export const selectFetchError = (state: RootState, problemId: IProblem['id']) =>
    (state.problemTestsets[problemId] || {}).fetchError;

export const selectTestsets = (state: RootState, problemId: IProblem['id']) =>
    (state.problemTestsets[problemId] || {}).testsets || [];

export const selectTestsetFetchTestsStarted = (
    state: RootState,
    problemId: IProblem['id'],
    testMapId: string,
) => {
    const map = getTestsetMapByProblemId(state, problemId);

    if (isUndefined(map)) {
        return undefined;
    }

    return (map[testMapId] || {}).fetchTestsStarted;
};

export const selectTestsetFetchTestsError = (
    state: RootState,
    problemId: IProblem['id'],
    testMapId: string,
) => {
    const map = getTestsetMapByProblemId(state, problemId);

    if (isUndefined(map)) {
        return undefined;
    }

    return (map[testMapId] || {}).fetchTestsError;
};

export const selectTestsetUpdateTestsStarted = (
    state: RootState,
    problemId: IProblem['id'],
    testMapId: string,
) => {
    const map = getTestsetMapByProblemId(state, problemId);

    if (isUndefined(map)) {
        return;
    }

    return (map[testMapId] || {}).updateTestsStarted;
};

const selectTestset = (state: RootState, problemId: IProblem['id'], testMapId: string) => {
    const map = getTestsetMapByProblemId(state, problemId);

    if (isUndefined(map)) {
        return undefined;
    }

    return (map[testMapId] || {}).data;
};

export const selectTestsetWithValidity = (
    state: RootState,
    problemId: IProblem['id'],
    testMapId: string,
) => {
    const testset = selectTestset(state, problemId, testMapId);

    if (isUndefined(testset)) {
        return undefined;
    }

    const validatedTests = selectValidationResultsTests(state, problemId);

    return {
        ...testset,
        tests: testset.tests.map((test) => ({
            ...test,
            isValid: getIsValidTestByPath(validatedTests, test.inputPath),
        })),
    };
};

export const selectAddSampleTestsetStarted = (state: RootState, problemId: IProblem['id']) =>
    (state.problemTestsets[problemId] || {}).addSampleStarted;
export const selectAddSampleTestsetError = (state: RootState, problemId: IProblem['id']) =>
    (state.problemTestsets[problemId] || {}).addSampleError;

export const getTestsetMapByProblemId = (state: RootState, problemId: IProblem['id']) =>
    (state.problemTestsets[problemId] || {}).testsetsMap;

export const selectAddTestsetStarted = (state: RootState, problemId: IProblem['id']) =>
    (state.problemTestsets[problemId] || {}).addTestsetStarted;

export const getIsValidTestByPath = (
    validatedTests: IValidatedTest[],
    path: IValidatedTest['path'],
) => {
    const testsWithPath = validatedTests.filter((test) => test.path === path);

    return testsWithPath.every((test) => test.isValid);
};
