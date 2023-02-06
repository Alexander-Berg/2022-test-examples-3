import { throttleRafDecorator, ThrottledRaf } from '../src';

class Class {
    @throttleRafDecorator
    public test() {
        // nope
    }
}

describe('ThrottleRaf', () => {
    describe('#throttleRaf', () => {
        it('Should replace function with throttled', () => {
            const instance = new Class();

            // eslint-disable-next-line @typescript-eslint/no-explicit-any
            expect((instance.test as ThrottledRaf<any>).cancel).toBeInstanceOf(Function);
        });
    });
});
