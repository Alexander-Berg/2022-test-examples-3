jest.dontMock('../../baseFilterManager');
const {validateValue, getDefaultValue} =
    require.requireActual('../../carriers').default;

const options = [
    {
        id: '1',
        title: 'Победа',
    },
    {
        id: '2',
        title: 'Аэрофлот',
    },
    {
        id: '3',
        title: 'Уральские авиалинии',
    },
];

describe('carriers', () => {
    describe('validateValue', () => {
        it('should return the same value if all of its parts are present in options', () => {
            const value = ['1', '2'];

            expect(validateValue(value, options)).toEqual(value);
        });

        it('should filter a value if some of its parts are not present in options', () => {
            const value = ['-1', '0', '1'];

            expect(validateValue(value, options)).toEqual(['1']);
        });

        it('should return default value if all parts of value are not present in options', () => {
            const value = ['-123'];

            expect(validateValue(value, options)).toEqual(getDefaultValue());
        });
    });
});
