import { once } from '../once';

describe('#once', () => {
    it('fn should be called once', () => {
        const fn = jest.fn();
        const calle = once(fn);

        calle('test1', 'test2');
        calle('test3', 'test4');

        expect(fn).toBeCalledTimes(1);
        expect(fn).nthCalledWith(1, 'test1', 'test2');
    });
});
