jest.mock('../../../../app/secrets', () => ({ skSalt: 'sk-salt' }), { virtual: true });

import secrets from '../../../../app/secrets';
import sk from '../../../../app/helpers/sk';

jest.mock('@ps-int/ufo-server-side-commons/helpers/sk', () => ({
    check: jest.fn(),
    get: jest.fn()
}));
import skMock from '@ps-int/ufo-server-side-commons/helpers/sk';

describe('app/helpers/sk', () => {
    it('check - прокидка параметров (неавторизованный)', () => {
        sk.check({
            user: {
                id: 0
            },
            cookies: {}
        }, 'test-sk');
        const checkCalls = popFnCalls(skMock.check);
        expect(checkCalls.length).toEqual(1);
        expect(checkCalls[0].length).toEqual(1);
        expect(checkCalls[0][0]).toEqual({
            sk: 'test-sk',
            uid: 0,
            yandexuid: undefined,
            salt: secrets.skSalt
        });
    });

    it('check - прокидка параметров (авторизованный)', () => {
        sk.check({
            user: {
                id: 123
            },
            cookies: {
                yandexuid: '456'
            }
        }, 'test-sk');
        const checkCalls = popFnCalls(skMock.check);
        expect(checkCalls.length).toEqual(1);
        expect(checkCalls[0].length).toEqual(1);
        expect(checkCalls[0][0]).toEqual({
            sk: 'test-sk',
            uid: 123,
            yandexuid: '456',
            salt: secrets.skSalt
        });
    });

    it('get - прокидка параметров (неавторизованный)', () => {
        sk.get({
            user: {
                id: 0
            },
            cookies: {}
        });
        const checkCalls = popFnCalls(skMock.get);
        expect(checkCalls.length).toEqual(1);
        expect(checkCalls[0].length).toEqual(1);
        expect(checkCalls[0][0]).toEqual({
            uid: 0,
            yandexuid: undefined,
            salt: secrets.skSalt
        });
    });

    it('get - прокидка параметров (авторизованный)', () => {
        sk.get({
            user: {
                id: 123
            },
            cookies: {
                yandexuid: '456'
            }
        });
        const checkCalls = popFnCalls(skMock.get);
        expect(checkCalls.length).toEqual(1);
        expect(checkCalls[0].length).toEqual(1);
        expect(checkCalls[0][0]).toEqual({
            uid: 123,
            yandexuid: '456',
            salt: secrets.skSalt
        });
    });
});
