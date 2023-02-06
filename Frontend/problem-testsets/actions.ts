import { createAsyncAction, createStandardAction, ActionType } from 'typesafe-actions';

import {
    IAddTestsetFailure,
    IAddTestsetRequest,
    IAddTestsetResponse,
    IFetchFailure,
    IFetchRequest,
    IFetchResponse,
    IFetchTestsetFailure,
    IFetchTestsetRequest,
    IFetchTestsetResponse,
    IRemoveFailure,
    IRemoveRequest,
    IRemoveResponse,
    IUpdateRequest,
} from 'client/store/problem-testsets/types';

export const fetchTestsets = createAsyncAction(
    'problems/testsets/GET_REQUEST',
    'problems/testsets/GET_SUCCESS',
    'problems/testsets/GET_FAILURE',
)<IFetchRequest, IFetchResponse, IFetchFailure>();

export const refreshTestsets = createStandardAction(
    'problems/testsets/REFRESH_REQUEST',
)<IFetchRequest>();

export const updateTestset = createAsyncAction(
    'problems/testsets/UPDATE_REQUEST',
    'problems/testsets/UPDATE_SUCCESS',
    'problems/testsets/UPDATE_FAILURE',
)<IUpdateRequest, IFetchTestsetResponse, IFetchTestsetFailure>();

export const fetchTestset = createAsyncAction(
    'problems/testsets/GET_TESTS_REQUEST',
    'problems/testsets/GET_TESTS_SUCCESS',
    'problems/testsets/GET_TESTS_FAILURE',
)<IFetchTestsetRequest, IFetchTestsetResponse, IFetchTestsetFailure>();

export const removeTestset = createAsyncAction(
    'problems/testsets/DELETE_REQUEST',
    'problems/testsets/DELETE_SUCCESS',
    'problems/testsets/DELETE_FAILURE',
)<IRemoveRequest, IRemoveResponse, IRemoveFailure>();

export const addTestset = createAsyncAction(
    'problems/testsets/ADD_REQUEST',
    'problems/testsets/ADD_SUCCESS',
    'problems/testsets/ADD_FAILURE',
)<IAddTestsetRequest, IAddTestsetResponse, IAddTestsetFailure>();

export const addSample = createAsyncAction(
    'problems/testsets/ADD_EXAMPLE_REQUEST',
    'problems/testsets/ADD_EXAMPLE_SUCCESS',
    'problems/testsets/ADD_EXAMPLE_FAILURE',
)<IAddTestsetRequest, IAddTestsetResponse, IAddTestsetFailure>();

const actions = {
    addSample,
    addTestset,
    fetchTestsets,
    refreshTestsets,
    updateTestset,
    fetchTestset,
    removeTestset,
};

export type ProblemTestsetsActions = ActionType<typeof actions>;

export default actions;
