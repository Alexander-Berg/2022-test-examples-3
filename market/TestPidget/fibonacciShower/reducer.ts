import {Value} from '../types';
import {FIBONACCI_LOAD_NEXT_SUCCESS} from './actions';

export const fibonacciReducer = (state: Value['fibonacciNumber'] = 0, action: any) => {
    if (action.type !== FIBONACCI_LOAD_NEXT_SUCCESS) {
        return state;
    }
    return action.payload;
};
