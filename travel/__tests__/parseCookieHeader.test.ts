import parseCookieHeader from '../parseCookieHeader';

describe('parseCookieHeader', () => {
    it('Вернет пустой объект для пустого значения', () => {
        expect(parseCookieHeader('')).toStrictEqual({});
    });

    it('Вернет заполненный объект', () => {
        expect(parseCookieHeader('key1=10; key2=0;key3=')).toStrictEqual({
            key1: '10',
            key2: '0',
            key3: '',
        });
    });

    it('Проигнорирует значение с пустым ключом', () => {
        expect(parseCookieHeader('key=2;=4;')).toStrictEqual({
            key: '2',
        });
    });
});
