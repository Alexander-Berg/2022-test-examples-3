import React from 'react';
import {useDispatch, useSelector} from '~/pidgets/root';
import {Value} from './types';
import {actions as fibonacciNumberActions} from './fibonacciShower/actions';

export const View = () => {
    const fibonacciNumber = useSelector((x: Value) => x.fibonacciNumber);
    const dispatch = useDispatch();

    return (
        <div>
            <span data-e2e="fibonacciNumber">{fibonacciNumber}</span>
            <button data-e2e="loadNextFibonacciNumber" onClick={() => dispatch(fibonacciNumberActions.loadNext())}>
                next fibonacci number
            </button>
        </div>
    );
};
