import * as utils from '../utils';

describe('#getRandomInteger', () => {
    it('returns integer between range', () => {
        const lowerInclusive = 0;
        const upperExclusive = 10;

        const value = utils.getRandomInteger(lowerInclusive, upperExclusive);

        expect(Number.isInteger(value)).toBeTruthy();
        expect(value).toBeGreaterThanOrEqual(lowerInclusive);
        expect(value).toBeLessThan(upperExclusive);
    });
});

describe('#delay', () => {
    beforeEach(() => {
        jest.useFakeTimers();
    });

    it('resolves promise without setTimeout calling', async() => {
        const delayed = utils.delay(0);

        expect(setTimeout).not.toBeCalled();

        await expect(delayed).resolves.toBeUndefined();
    });

    it('resolves promise and calls setTimeout with correct arguments', async() => {
        const delayed = utils.delay(10);

        expect(setTimeout).toBeCalledWith(expect.any(Function), 10000);

        jest.runOnlyPendingTimers();

        await expect(delayed).resolves.toBeUndefined();
    });

    it('resolves promise calls setTimeout with max timeout value', async() => {
        const delayed = utils.delay(50);

        expect(setTimeout).toBeCalledWith(expect.any(Function), 30000);

        jest.runOnlyPendingTimers();

        await expect(delayed).resolves.toBeUndefined();
    });
});
