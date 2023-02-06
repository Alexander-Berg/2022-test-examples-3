import { memoize } from '../memoize/memoize';
import { ICache } from '../memoize/types';

const getCache = <T>(returnValue?: T): ICache<T> => {
    return {
        add: jest.fn(),
        // @ts-ignore
        get: jest.fn(key => {
            if (returnValue) {
                return { key, value: returnValue };
            }
        }),
        remove: jest.fn(),
    };
};

describe('Функция memoize', () => {
    it('Вызывает оригинал', () => {
        const original = jest.fn();
        const wrapper = memoize(original, { cache: getCache() });

        wrapper(1, 2, 3);

        expect(original).toBeCalledWith(1, 2, 3);
    });

    it('Сохраняет значение в кеше, используя первый аргумент как ключ', () => {
        const cache: ICache<string> = getCache();
        const original = jest.fn((_1, _2) => 'value');
        const wrapper = memoize(original, { cache });

        wrapper('key', 'another arg');

        expect(cache.add).toBeCalledWith({ key: 'key', value: 'value' });
    });

    it('Возвращает значение из кеша, если оно есть', () => {
        const cache = getCache('from cache');
        const original = jest.fn();
        const wrapper = memoize(original, { cache });

        const result = wrapper('key', 2, 3);

        expect(cache.get).toBeCalledWith('key');
        expect(original).not.toBeCalled();
        expect(result).toEqual('from cache');
    });

    it('Вычислет ключ, вызывая переданную функцию', () => {
        const hashFn = jest.fn((...args) => args.join(','));
        const cache = getCache();
        const wrapper = memoize(jest.fn(), { cache, hashFn });

        wrapper('hello', 'world');

        expect(hashFn).toBeCalledWith('hello', 'world');
        expect(cache.add).toBeCalledWith({ key: 'hello,world', value: undefined });
    });
});
