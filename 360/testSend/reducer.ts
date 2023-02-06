import { createReducer } from '@reduxjs/toolkit';

import { ITestSendState } from './types';
import {
    testSend,
    testSendAbort,
    testSendStart,
    testSendSuccess,
    testSendFailure,
} from './actions';

const INITIAL_STATE: ITestSendState = {
    isActive: false,
    isLoading: false,
    error: null,
};

export const testSendReducer = createReducer(INITIAL_STATE, builder => {
    builder
        .addCase(testSend, () => ({
            ...INITIAL_STATE,
            isActive: true,
        }))
        .addCase(testSendAbort, state => ({
            ...state,
            isActive: false,
        }))
        .addCase(testSendStart, state => ({
            ...state,
            isLoading: true,
            error: null,
        }))
        .addCase(testSendSuccess, state => ({
            ...state,
            isLoading: false,
            error: null,
            isActive: false,
        }))
        .addCase(testSendFailure, (state, action) => ({
            ...state,
            isActive: Boolean(action.payload?.detail?.recipients),
            isLoading: false,
            error: action.payload?.detail?.recipients || null,
        }));
});
