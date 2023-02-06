import { FEATURE_NAME } from './constants';

export interface ITestSendState {
    isActive: boolean;
    isLoading: boolean;
    error: string | null;
}

export interface IReduxState {
    [FEATURE_NAME]: ITestSendState;
}
