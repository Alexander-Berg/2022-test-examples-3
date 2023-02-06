import stringifyCookies from '../stringifyCookies';

describe('stringifyCookies', () => {
    it('Для пустого объекта вернет пустую строку', () => {
        expect(stringifyCookies({})).toBe('');
    });

    it('Вернет заполненную строку', () => {
        expect(
            stringifyCookies({
                key1: '20',
                key2: '',
                key3: '0',
            }),
        ).toBe('key1=20; key2=; key3=0');
    });
});
