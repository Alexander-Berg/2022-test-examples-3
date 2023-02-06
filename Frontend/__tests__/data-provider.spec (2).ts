import { IDataAccess } from '@yandex-turbo/core/turbo-app/store.types';
import { DataProvider, EMethodTypes } from '../data-provider';
import * as fetchLib from '~/libs/fetch';

const fakeCache: IDataAccess<string> = {
    get: () => Promise.resolve(),
    add: () => Promise.resolve(),
    update: () => Promise.resolve(),
    remove: () => Promise.resolve(),
};

const dataProvider = new DataProvider<string>({
    cache: fakeCache,
    parentReqId: '123-456-test-req-id',
});

describe('DataProvider', () => {
    let fetch: jest.SpyInstance;
    let dispatch: jest.Mock;

    beforeEach(() => {
        fetch = jest.spyOn(fetchLib, 'default');
        fetch.mockImplementation(jest.fn().mockImplementation(() => Promise.resolve({
            ok: true,
            status: 200,
            json: () => Promise.resolve({ test: true }),
        })));

        dispatch = jest.fn();

        global.Ya = {
            store: {
                dispatch,
            },
        };
    });

    afterEach(() => {
        fetch.mockClear();
        dispatch.mockClear();
    });

    describe('request', () => {
        it('Вызывает fetch с корректными параметрами', () => {
            dataProvider.request(
                EMethodTypes.POST,
                'https://test-turbo-forms.common.yandex.ru/submit/shopping-cart/'
            );

            expect(fetch).toBeCalledWith('https://test-turbo-forms.common.yandex.ru/submit/shopping-cart/', {
                body: '',
                credentials: 'include',
                headers: {
                    'Content-Type': 'application/json'
                },
                method: 'POST',
                mode: 'cors'
            });
        });

        it('Триггерит ошибку, если пришёл некорректный ответ', async() => {
            fetch.mockResolvedValue({
                ok: false,
                status: 503,
            });

            expect.assertions(1);

            await dataProvider.request(EMethodTypes.GET, 'https://test-url.com')
                .catch(e => expect(e.status).toStrictEqual(503));
        });

        it('Метод GET диспатчит корректный экшн', async() => {
            await dataProvider.request(EMethodTypes.GET, 'https://test-url.com');

            expect(dispatch).toBeCalledWith({
                type: '@@meta/DATA_RECEIVED',
                payload: {
                    data: {
                        test: true,
                        statusCode: 200,
                    },
                },
            });
        });

        it('Передает заголовок X-Req-Id, если указана опция sendReqIdHeader', () => {
            dataProvider.request(
                EMethodTypes.POST,
                'https://test-turbo-forms.common.yandex.ru/submit/shopping-cart/',
                '',
                { sendReqIdHeader: true },
            );

            expect(fetch).toBeCalledWith('https://test-turbo-forms.common.yandex.ru/submit/shopping-cart/', {
                body: '',
                credentials: 'include',
                headers: {
                    'Content-Type': 'application/json',
                    'X-Req-Id': '123-456-test-req-id',
                },
                method: 'POST',
                mode: 'cors'
            });
        });
    });
});
