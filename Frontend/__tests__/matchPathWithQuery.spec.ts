import { matchPathWithQuery } from '../matchPathWithQuery';

describe('matchPathWithQuery', () => {
    const location = {
        pathname: '/turbo',
        search: '?text=mirm.ru%2Fyandexturbocatalog%2F&category_id=123',
        state: {},
        hash: '',
    };

    const props = {
        path: '/turbo',
        query: {
            text: /\/yandexturbocatalog\/$/,
            category_id: '123',
        },
    };

    it('Возвращает корректный результат, если указан query', () => {
        expect(matchPathWithQuery(location, props)).toHaveProperty('query', {
            text: 'mirm.ru/yandexturbocatalog/',
            category_id: '123',
        });
    });

    it('Возвращает null, если query не матчится по регулярке', () => {
        expect(matchPathWithQuery({
            ...location,
            search: '?text=mirm.ru%2Fyandexturbocart%2F&category_id=123',
        }, props)).toBeNull();
    });

    it('Возвращает null, если query не матчится по строке', () => {
        expect(matchPathWithQuery({
            ...location,
            search: '?text=mirm.ru%2Fyandexturbocatalog%2F&category_id=456',
        }, props)).toBeNull();
    });

    it('Возвращает корректный результат, если query не указан', () => {
        expect(matchPathWithQuery(location, {
            ...props,
            query: undefined,
        })).toHaveProperty('query', {
            text: 'mirm.ru/yandexturbocatalog/',
            category_id: '123',
        });
    });

    it('Возвращает null, если pathname не совпадают', () => {
        expect(matchPathWithQuery(location, {
            ...props,
            path: '/404',
        })).toBeNull();
    });
});
