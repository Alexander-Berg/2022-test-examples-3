import { getInitialFilters } from './getInitialFilters';

const defaultQuery = {
    cities: '1',
    professions: ['1', '2'],
    dump: 'dump',
};

describe('Получение значения фильтров из query строки', () => {
    it('Должны получить значения фильтров', () => {
        const initialFilters = getInitialFilters(defaultQuery);
        expect(initialFilters).toEqual({
            cities: ['1'],
            public_professions: ['1', '2'],
        });
    });

    it('Должны получить пустой объект, если в query параметрах отсутствуют значения фильтров', () => {
        const initialFilters = getInitialFilters({
            dump: defaultQuery.dump,
        });

        expect(initialFilters).toEqual({});
    });
});
