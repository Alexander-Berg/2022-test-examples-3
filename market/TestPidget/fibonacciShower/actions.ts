export const FIBONACCI_LOAD_NEXT = 'TestPidget_fibonacciLoadNext';
export const FIBONACCI_LOAD_NEXT_SUCCESS = 'TestPidget_fibonacciLoadNextSuccess';

export const actions = {
    loadNext: () => ({
        type: FIBONACCI_LOAD_NEXT,
    }),

    loadNextSuccess: (nextFib: number) => ({
        type: FIBONACCI_LOAD_NEXT_SUCCESS,
        payload: nextFib,
    }),
};
