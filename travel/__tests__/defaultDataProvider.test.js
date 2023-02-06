import makeExternalUrl from '../../url/makeExternalUrl';

const makeUrlResult =
    '//suggests.rasp.yandex.net/all_suggests?language=ru&part=%D0%95%D0%BA%D0%B0%D1%82';

jest.mock('../../url/makeExternalUrl');
makeExternalUrl.mockImplementation(() => makeUrlResult);

const part = 'Екат';
const options = {
    url: '//suggests.rasp.yandex.net/',
    path: 'all_suggests',
    query: {
        language: 'ru',
    },
};

const makeUrlUrl = '//suggests.rasp.yandex.net/all_suggests';
const makeUrlQuery = {
    language: 'ru',
    part: 'Екат',
};

const getItemsFromResponseResult = [
    {
        value: {
            key: 'c54',
            title: 'Екатеринбург',
        },
        text: 'г. Екатеринбург',
    },
];
const getItemsFromResponse = jest.fn(() => getItemsFromResponseResult);

jest.setMock('../getItemsFromResponse', getItemsFromResponse);

const fakeFetch = jest.fn();
const oldFetch = global.fetch;
const response = [part, ['c54', 'Екатеринбург', 'г. Екатеринбург']];

const defaultDataProvider = require.requireActual(
    '../defaultDataProvider',
).default;

describe('defaultDataProvider', () => {
    beforeEach(() => {
        fakeFetch.mockImplementation(() =>
            Promise.resolve({
                json: () => Promise.resolve(response),
            }),
        );
        global.fetch = fakeFetch;
    });

    afterEach(() => {
        global.fetch = oldFetch;
    });

    it('should build an url to request data', () => {
        defaultDataProvider(part, options);
        expect(makeExternalUrl).toBeCalledWith(makeUrlUrl, makeUrlQuery);
    });

    it('should initiate a fetch request with an url built', () => {
        defaultDataProvider(part, options);

        const [fetchUrl] = fakeFetch.mock.calls[0];

        expect(fetchUrl).toBe(makeUrlResult);
    });

    describe('returned promise', () => {
        it('should resolve with suggest items, if request was successful', () =>
            defaultDataProvider(part, options).then(items => {
                expect(getItemsFromResponse).toBeCalledWith(response);

                // TODO: #новыеурлы вернуть toBe вместо toEqual при появлении slug в саджестах
                expect(items).toEqual(getItemsFromResponseResult);
            }));

        it('should reject with request error, if jsonp request has failed', () => {
            const requestError = new Error('request error');

            fakeFetch.mockImplementation(() => Promise.reject(requestError));

            return defaultDataProvider(part, options).catch(error => {
                expect(error).toBe(requestError);
            });
        });
    });
});
