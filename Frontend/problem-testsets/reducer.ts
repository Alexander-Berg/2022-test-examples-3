import { getType } from 'typesafe-actions';

import { generateTestsetMapKey } from 'common/utils/helpers/problem-testsets';

import {
    addSample,
    addTestset,
    fetchTestset,
    fetchTestsets,
    refreshTestsets,
    removeTestset,
    updateTestset,
    ProblemTestsetsActions,
} from 'client/store/problem-testsets/actions';
import { getTestsetsList, getTestsetMap } from 'client/store/problem-testsets/helpers';
import { IProblemTestsetStore } from 'client/store/problem-testsets/types';

const initialState: IProblemTestsetStore = {};

export default function problemTestsetsReducer(
    state: IProblemTestsetStore = initialState,
    action: ProblemTestsetsActions,
) {
    switch (action.type) {
        case getType(fetchTestsets.request): {
            const { problemId } = action.payload;
            const problemTestsetsState = state[problemId] || {};

            return {
                ...state,
                [problemId]: {
                    ...problemTestsetsState,
                    fetchStarted: true,
                    fetchError: null,
                },
            };
        }
        case getType(fetchTestsets.success): {
            const { problemId, data } = action.payload;
            const problemTestsetsState = state[problemId] || {};

            return {
                ...state,
                [problemId]: {
                    ...problemTestsetsState,
                    fetchStarted: false,
                    fetchError: null,
                    testsets: getTestsetsList(data),
                    testsetsMap: getTestsetMap(data, problemId),
                },
            };
        }
        case getType(fetchTestsets.failure): {
            const { problemId, error } = action.payload;
            const problemTestsetsState = state[problemId] || {};

            return {
                ...state,
                [problemId]: {
                    ...problemTestsetsState,
                    fetchStarted: false,
                    fetchError: error,
                },
            };
        }
        case getType(refreshTestsets): {
            const { problemId } = action.payload;
            const problemTestsetsState = state[problemId] || {};

            return {
                ...state,
                [problemId]: {
                    ...problemTestsetsState,
                    fetchStarted: false,
                    fetchError: null,
                },
            };
        }
        case getType(fetchTestset.request): {
            const { problemId, testsetId } = action.payload;
            const problemTestsetsState = state[problemId] || {};
            const { testsetsMap: savedTestsetsMap = {} } = problemTestsetsState;
            const id = generateTestsetMapKey(problemId, testsetId);

            return {
                ...state,
                [problemId]: {
                    ...problemTestsetsState,
                    testsetsMap: {
                        ...savedTestsetsMap,
                        [id]: {
                            ...savedTestsetsMap[id],
                            fetchTestsStarted: true,
                            fetchTestsError: null,
                        },
                    },
                },
            };
        }
        case getType(fetchTestset.success): {
            const { problemId, testsetId, data } = action.payload;
            const problemTestsetsState = state[problemId] || {};
            const { testsetsMap: savedTestsetsMap = {} } = problemTestsetsState;
            const id = generateTestsetMapKey(problemId, testsetId);

            return {
                ...state,
                [problemId]: {
                    ...problemTestsetsState,
                    testsetsMap: {
                        ...savedTestsetsMap,
                        [id]: {
                            data,
                            fetchTestsStarted: false,
                            fetchTestsError: null,
                        },
                    },
                },
            };
        }
        case getType(fetchTestset.failure): {
            const { problemId, testsetId, error } = action.payload;
            const problemTestsetsState = state[problemId] || {};
            const { testsetsMap: savedTestsetsMap = {} } = problemTestsetsState;
            const id = generateTestsetMapKey(problemId, testsetId);

            return {
                ...state,
                [problemId]: {
                    ...problemTestsetsState,
                    testsetsMap: {
                        ...savedTestsetsMap,
                        [id]: {
                            ...savedTestsetsMap[id],
                            fetchTestsStarted: false,
                            fetchTestsError: error,
                        },
                    },
                },
            };
        }
        case getType(updateTestset.request): {
            const { problemId, testsetId } = action.payload;
            const problemTestsetsState = state[problemId] || {};
            const { testsetsMap: savedTestsetsMap = {} } = problemTestsetsState;
            const id = generateTestsetMapKey(problemId, testsetId);

            return {
                ...state,
                [problemId]: {
                    ...problemTestsetsState,
                    testsetsMap: {
                        ...savedTestsetsMap,
                        [id]: {
                            ...savedTestsetsMap[id],
                            updateTestsStarted: true,
                            updateTestsError: null,
                        },
                    },
                },
            };
        }
        case getType(updateTestset.success): {
            const { problemId, testsetId, data } = action.payload;
            const problemTestsetsState = state[problemId] || {};
            const { testsetsMap: savedTestsetsMap = {} } = problemTestsetsState;
            const id = generateTestsetMapKey(problemId, testsetId);

            return {
                ...state,
                [problemId]: {
                    ...problemTestsetsState,
                    testsetsMap: {
                        ...savedTestsetsMap,
                        [id]: {
                            data,
                            updateTestsStarted: false,
                            updateTestsError: null,
                        },
                    },
                },
            };
        }
        case getType(updateTestset.failure): {
            const { problemId, testsetId, error } = action.payload;
            const problemTestsetsState = state[problemId] || {};
            const { testsetsMap: savedTestsetsMap = {} } = problemTestsetsState;
            const id = generateTestsetMapKey(problemId, testsetId);

            return {
                ...state,
                [problemId]: {
                    ...problemTestsetsState,
                    testsetsMap: {
                        ...savedTestsetsMap,
                        [id]: {
                            ...savedTestsetsMap[id],
                            updateTestsStarted: false,
                            updateTestsError: error,
                        },
                    },
                },
            };
        }
        case getType(removeTestset.request): {
            const { problemId } = action.payload;
            const problemTestsetsState = state[problemId] || {};

            return {
                ...state,
                [problemId]: {
                    ...problemTestsetsState,
                    removeStarted: true,
                    removeError: null,
                },
            };
        }
        case getType(removeTestset.success): {
            const { problemId } = action.payload;
            const problemTestsetsState = state[problemId] || {};

            return {
                ...state,
                [problemId]: {
                    ...problemTestsetsState,
                    removeStarted: false,
                    removeError: null,
                },
            };
        }
        case getType(removeTestset.failure): {
            const { problemId, error } = action.payload;
            const problemTestsetsState = state[problemId] || {};

            return {
                ...state,
                [problemId]: {
                    ...problemTestsetsState,
                    removeStarted: false,
                    removeError: error,
                },
            };
        }
        case getType(addSample.request): {
            const { problemId } = action.payload;
            const problemTestsetsState = state[problemId] || {};

            return {
                ...state,
                [problemId]: {
                    ...problemTestsetsState,
                    addSampleStarted: true,
                    addSampleError: null,
                },
            };
        }
        case getType(addSample.success): {
            const { problemId } = action.payload;
            const problemTestsetsState = state[problemId] || {};

            return {
                ...state,
                [problemId]: {
                    ...problemTestsetsState,
                    addSampleStarted: false,
                    addSampleError: null,
                },
            };
        }
        case getType(addSample.failure): {
            const { problemId, error } = action.payload;
            const problemTestsetsState = state[problemId] || {};

            return {
                ...state,
                [problemId]: {
                    ...problemTestsetsState,
                    addSampleStarted: false,
                    addSampleError: error,
                },
            };
        }
        case getType(addTestset.request): {
            const { problemId } = action.payload;
            const problemTestsetsState = state[problemId] || {};

            return {
                ...state,
                [problemId]: {
                    ...problemTestsetsState,
                    addTestsetStarted: true,
                    addTestsetError: null,
                },
            };
        }
        case getType(addTestset.success): {
            const { problemId } = action.payload;
            const problemTestsetsState = state[problemId] || {};

            return {
                ...state,
                [problemId]: {
                    ...problemTestsetsState,
                    addTestsetStarted: false,
                    addTestsetError: null,
                },
            };
        }
        case getType(addTestset.failure): {
            const { problemId, error } = action.payload;
            const problemTestsetsState = state[problemId] || {};

            return {
                ...state,
                [problemId]: {
                    ...problemTestsetsState,
                    addTestsetStarted: false,
                    addTestsetError: error,
                },
            };
        }
        default: {
            return state;
        }
    }
}
