import type {PidgetState} from '~/pidgets/root';
import {Value} from './types';

export type State = PidgetState<Value>;

export const makeInitialState = (): State => ({
    type: 'ready',
    value: {
        fibonacciNumber: 0,
    },
});
