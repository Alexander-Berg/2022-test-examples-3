import getSortFunction from '../getSortFunction';

describe('getSortFunction', () => {
    it('Вернет функцию для простой сортировки строк', () => {
        const arrayWithOrder = ['one', 'second', 'third', 4];

        expect(
            ['second', 'one', 4, 'third'].sort(getSortFunction(arrayWithOrder)),
        ).toEqual(arrayWithOrder);
    });

    it(`Поддерживает передачу регулярных выражений для сортировки строк. При этом, если две строки соответствуют одному
    регулярному выражению, то они будут сортироваться в алфавитном порядке`, () => {
        const arrayWithOrder = [
            'one',
            'second',
            'third',
            /^[ab]$/,
            'last',
            'not',
        ];

        expect(
            ['second', 'b', 'a', 'one', 'last', 'third'].sort(
                getSortFunction(arrayWithOrder),
            ),
        ).toEqual(['one', 'second', 'third', 'a', 'b', 'last']);
    });

    it('Элементы, которые отсутствуют в массиве с порядком, будут перемещены вниз с сохранением изначального порядка', () => {
        const arrayWithOrder = ['one', 'second', 'third'];

        expect(
            ['second', 'b', 'a', 'one', 'last', 'third'].sort(
                getSortFunction(arrayWithOrder),
            ),
        ).toEqual(['one', 'second', 'third', 'b', 'a', 'last']);
    });
});
