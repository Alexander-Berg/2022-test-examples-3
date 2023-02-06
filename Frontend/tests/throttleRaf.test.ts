import { throttleRaf } from '../src';
import { raf, caf } from './mocks';

describe('ThrottleRaf', () => {
    describe('#throttleRaf', () => {
        beforeAll(() => {
            // @ts-ignore
            Object.defineProperties(global.window, {
                requestAnimationFrame: { value: raf },
                cancelAnimationFrame: { value: caf },
            });
        });

        afterEach(() => {
            jest.clearAllMocks();
        });

        it('Should call', () => {
            const cb = jest.fn();

            throttleRaf(cb)();

            expect(raf.mock.calls.length).toEqual(1);
            expect(cb.mock.calls.length).toEqual(1);
        });

        it('Should call twice', () => {
            const cb = jest.fn();

            const call = throttleRaf(cb);

            call();
            call();

            expect(raf.mock.calls.length).toEqual(2);
            expect(cb.mock.calls.length).toEqual(2);
        });

        it('Should cancel', () => {
            const cb = jest.fn();

            throttleRaf(cb).cancel();

            expect(raf.mock.calls.length).toEqual(0);
            expect(caf.mock.calls.length).toEqual(1);
            expect(cb.mock.calls.length).toEqual(0);
        });
    });
});
