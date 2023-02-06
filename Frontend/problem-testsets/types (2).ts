import { IMaybeError } from 'common/types/error';
import { IProblem } from 'common/types/problem';
import { IProblemTest, IProblemTestset, IProblemTestsetBriefInfo } from 'common/types/problem-test';

import { IAPIMaybeError } from 'client/store/types';

export interface IFetchRequest {
    problemId: IProblem['id'];
}

export interface IFetchResponse {
    problemId: IProblem['id'];
    data: IProblemTestset[];
}

export interface IFetchFailure extends IAPIMaybeError {
    problemId: IProblem['id'];
}

export interface IUpdateRequest {
    problemId: IProblem['id'];
    testsetId: IProblemTestset['id'];
    data: Partial<IProblemTest>;
}

export interface IFetchTestsetRequest {
    problemId: IProblem['id'];
    testsetId: IProblemTestset['id'];
}

export interface IFetchTestsetResponse {
    problemId: IProblem['id'];
    testsetId: IProblemTestset['id'];
    data: IProblemTestset;
}

export interface IFetchTestsetFailure extends IAPIMaybeError {
    problemId: IProblem['id'];
    testsetId: IProblemTestset['id'];
}

export interface IRemoveRequest {
    problemId: IProblem['id'];
    testsetId: IProblemTestset['id'];
}

export interface IRemoveResponse {
    problemId: IProblem['id'];
}

export interface IRemoveFailure extends IAPIMaybeError {
    problemId: IProblem['id'];
}

export interface IAddTestsetRequest {
    problemId: IProblem['id'];
}

export interface IAddTestsetResponse {
    problemId: IProblem['id'];
}

export interface IAddTestsetFailure extends IAPIMaybeError {
    problemId: IProblem['id'];
}

export interface ITestsetsMap {
    [testsetId: string]: {
        data: IProblemTestset;
        fetchTestsStarted?: boolean;
        fetchTestsError?: IMaybeError;
        updateTestsStarted?: boolean;
        updateTestsError?: IMaybeError;
    };
}

export interface IProblemTestsetStore {
    [problemId: string]: {
        fetchStarted: boolean;
        fetchError: IMaybeError;
        removeStarted: boolean;
        removeError: IMaybeError;
        testsets: IProblemTestsetBriefInfo[];
        testsetsMap: ITestsetsMap;
        addTestsetStarted: boolean;
        addTestsetError: IMaybeError;
        addSampleStarted: boolean;
        addSampleError: IMaybeError;
    };
}
