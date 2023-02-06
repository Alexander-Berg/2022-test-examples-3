import { Timer } from '../Timer';

describe('#Timer', () => {
    it('should exec after timeout', async () => {
        const spy = jest.fn();

        jest.useFakeTimers();

        // eslint-disable-next-line no-new
        new Timer(spy, 10);

        expect(spy).toBeCalledTimes(0);

        jest.runAllTimers();

        expect(spy).toBeCalledTimes(1);
    });

    it('should be cancelled', async () => {
        const spy = jest.fn();

        jest.useFakeTimers();

        const timer = new Timer(spy, 10);

        expect(spy).toBeCalledTimes(0);

        timer.cancel();

        jest.runAllTimers();

        expect(spy).toBeCalledTimes(0);
    });
});
