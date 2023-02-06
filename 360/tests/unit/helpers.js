export const popFnCalls = (jestFn) => {
    const calls = jestFn.mock.calls.slice();
    jestFn.mockClear();

    return calls;
};
