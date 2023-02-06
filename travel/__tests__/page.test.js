jest.disableAutomock();

import {SEARCH} from '../../routes/search';
import {HOME_PAGE_NAME} from '../../routes/index';

import {isSearch} from '../page';

describe('Функция isSearch', () => {
    it('Должна возвращать "true" если текущая страница - страница поиска', () => {
        const result = isSearch({page: {current: SEARCH}});

        expect(result).toBe(true);
    });

    it('Должна возвращать "false" если текущая страница - не страница поиска', () => {
        const result = isSearch({page: {current: HOME_PAGE_NAME}});

        expect(result).toBe(false);
    });

    it('Должна возвращать "true" если текущая загружаемая страница - страница поиска', () => {
        const result = isSearch({page: {fetching: SEARCH}}, true);

        expect(result).toBe(true);
    });
});
