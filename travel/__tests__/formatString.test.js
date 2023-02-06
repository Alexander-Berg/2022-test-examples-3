const capitalizeFirstLetter =
    require.requireActual('../formatString').capitalizeFirstLetter;

describe('capitalizeFirstLetter', () => {
    it('Сделает первую букву заглавной', () => {
        expect(capitalizeFirstLetter('text')).toBe('Text');
    });

    it('Ничего не изменит для текстов в uppercase', () => {
        expect(capitalizeFirstLetter('TEXT')).toBe('TEXT');
    });

    it('Ничего не изменит для пустых текстов', () => {
        expect(capitalizeFirstLetter('')).toBe('');
    });
});
