import { debounce } from '../debounce';

describe('debounce', () => {
    let testFunction: ReturnType<typeof jest.fn>;

    const testTime = 10;
    const wait = (time: number) => new Promise((res) => setTimeout(() => res(), time));

    beforeEach(() => {
        testFunction = jest.fn();
        const testDebouncedFunction = debounce(testFunction, testTime);

        testDebouncedFunction();
        testDebouncedFunction();
    });

    it('wrapped function should be called once', async () => {
        await wait(testTime);

        expect(testFunction).toHaveBeenCalledTimes(1);
    });

    it('wrapped function should not be called immediately', () => {
        expect(testFunction).toHaveBeenCalledTimes(0);
    });
});
