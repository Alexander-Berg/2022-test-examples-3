import metaTags from '../metaTags';
import searchPage from '../metaPages/search';
import defaultPage from '../metaPages/default';

jest.mock('../metaPages/search');
jest.mock('../metaPages/default');

const defaultState = {
    language: 'ru',
};

const searchPageMeta = {
    title: 'Search Page title',
    meta: [{name: 'description', content: 'Search Page Description'}],
};

const defaultPageMeta = {
    title: 'Default Title',
    meta: [{name: 'description', content: 'Default description'}],
};

describe('metaTags', () => {
    it('Get meta information for Search', () => {
        searchPage.mockReturnValue(searchPageMeta);
        const state = {
            ...defaultState,
            page: {
                current: 'search',
            },
        };

        expect(metaTags(state)).toEqual({
            ...searchPageMeta,
        });
        expect(searchPage).toBeCalledWith(state);
    });

    it('Get meta information for Error Page', () => {
        defaultPage.mockReturnValue(defaultPageMeta);
        const state = {
            ...defaultState,
            page: {
                current: 'error',
            },
        };

        expect(metaTags(state)).toEqual(defaultPageMeta);
        expect(defaultPage).toBeCalledWith(state);
        expect(searchPage).not.toBeCalled();
    });
});
