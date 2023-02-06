jest.disableAutomock();

import sortDirections from '../sortDirections';

describe('sortDirectoins', () => {
    const locale = 'ru';

    it('Сортирует лексикографически по полю title', () => {
        const directions = [
            {title: 'bbbccc'},
            {title: 'aaabcd'},
            {title: 'AAAbcd'},
            {title: 'bbcccc'},
        ];

        expect(sortDirections(directions, locale)).toEqual([
            {title: 'AAAbcd'},
            {title: 'aaabcd'},
            {title: 'bbbccc'},
            {title: 'bbcccc'},
        ]);
    });

    it('Возвращает новый массив', () => {
        const directions = [{title: 'aaa'}];

        expect(sortDirections(directions, locale)).not.toBe(directions);
    });
});
