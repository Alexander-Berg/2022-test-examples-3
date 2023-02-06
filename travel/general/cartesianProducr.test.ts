import product from './cartesianProduct';

describe('cartesianProduct', () => {
    it('should return an empty array when arguments are empty', function () {
        expect(product([])).toEqual([[]]);
    });

    it('should return the correct product', function () {
        const result = product([
            [1, 2],
            [3, 4],
        ]);
        expect(result).toHaveLength(4);
        expect(result).toEqual([
            [1, 3],
            [1, 4],
            [2, 3],
            [2, 4],
        ]);
    });
});
