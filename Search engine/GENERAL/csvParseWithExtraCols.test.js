import csvParseWithExtraCols from './csvParseWithExtraCols';

describe('csvParseWithExtraCols', () => {
    test('empty', () => {
        const actual = csvParseWithExtraCols('', {extraColumnName: 'extra'});
        expect(actual).toEqual([]);
    });

    test('extra cols', () => {
        const actual = csvParseWithExtraCols('col1,col2\n1,2\n1,2,3,4\n1,2,3', {
            extraColumnName: 'extra',
        });
        const expected = [
            {col1: '1', col2: '2', extra1: undefined, extra2: undefined},
            {col1: '1', col2: '2', extra1: '3', extra2: '4'},
            {col1: '1', col2: '2', extra1: '3', extra2: undefined},
        ];
        expect(actual).toEqual(expected);
    });
});
