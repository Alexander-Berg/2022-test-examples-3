import { findKey } from '../findKey';

describe('findKey', () => {
    it('Найдёт ключ', () => {
        const key = findKey({ key: 'value' }, value => value === 'value');

        expect(key).toBe('key');
    });

    it('Вернёт undefined, если ключа нет', () => {
        const key = findKey({}, value => value === 'value');

        expect(key).toBeUndefined();
    });
});
