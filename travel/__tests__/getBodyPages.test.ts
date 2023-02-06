import {getBodyPages} from 'components/Pagination/utilities/getBodyPages';

describe('getBodyPages', () => {
    it('Вернёт пустой массив', () => {
        expect(getBodyPages([1, 2], 1, 5)).toEqual([]);
    });

    it('Вернёт массив страниц за исключением первого и последнего элемента', () => {
        expect(getBodyPages([1, 2, 3, 4, 5, 6, 7, 8, 9], 4, 5)).toEqual([
            2, 3, 4, 5, 6, 7, 8,
        ]);
    });

    it('Вернёт диапазон страниц примыкающий к началу', () => {
        expect(getBodyPages([1, 2, 3, 4, 5, 6, 7, 8, 9, 10], 4, 5)).toEqual([
            2, 3, 4, 5, 6, 7,
        ]);
    });

    it('Вернёт диапазон страниц примыкающих к концу', () => {
        expect(getBodyPages([1, 2, 3, 4, 5, 6, 7, 8, 9, 10], 5, 5)).toEqual([
            4, 5, 6, 7, 8, 9,
        ]);
    });

    it('Вернёт диапазон страниц из середины', () => {
        expect(
            getBodyPages([1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12], 6, 5),
        ).toEqual([5, 6, 7, 8, 9]);
    });
});
