import cartesianProduct from '../cartesianProduct';

describe('cartesianProduct', () => {
    it('Вызов без параметров, вернет [[]]', () => {
        expect(cartesianProduct()).toEqual([[]]);
    });

    it('Вызов с двумя параметрами', () => {
        expect(cartesianProduct([1, 2], [true, false])).toEqual([
            [1, true],
            [2, true],
            [1, false],
            [2, false],
        ]);

        expect(cartesianProduct([1, 2], [true])).toEqual([
            [1, true],
            [2, true],
        ]);
    });

    it('Вызов с тремя параметрами', () => {
        expect(
            cartesianProduct([1, 2], [true, false], ['first', 'second']),
        ).toEqual([
            [1, true, 'first'],
            [2, true, 'first'],
            [1, false, 'first'],
            [2, false, 'first'],
            [1, true, 'second'],
            [2, true, 'second'],
            [1, false, 'second'],
            [2, false, 'second'],
        ]);
    });
});
