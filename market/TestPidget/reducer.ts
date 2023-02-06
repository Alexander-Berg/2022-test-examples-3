import {combineReducers} from 'redux';

import {dummyReducer} from '~/reducers';

import {fibonacciReducer} from './fibonacciShower/reducer';

export const reducer = combineReducers({
    type: dummyReducer,
    value: combineReducers({
        fibonacciNumber: fibonacciReducer,
    }),
});
