import nock from 'nock';

import API from '../../../../../src/API/index';
import raiseMocks from '../../../helpers/raise-mocks';

/**
 * @see {@link https://jestjs.io/docs/en/manual-mocks#mocking-user-modules}
 */
if (!process.env.LOG) {
    jest.mock('../../../../../utils/logs');
}

/* Mocks */
const shopsOpinionsMock = require('./__mocks__/shops-opinions');
const shopsOpinionsMockError = require('./__mocks__/shops-opinions-error');
const shopsMockInvalid = require('./__mocks__/shops-opinions-invalid');

describe('Get shops/opinions', () => {
    afterEach(() => {
        nock.cleanAll();
    });

    test('should fail validation', async () => {
        const mockScopes = raiseMocks(shopsOpinionsMock);
        let response;
        let errorCatched;
        try {
            response = await API.yandex.market['v2.1.5'].shops.opinions({});
        } catch (error) {
            errorCatched = true;
        }
        expect(errorCatched).toBeTruthy();
        expect(response).toBeUndefined();
        mockScopes.forEach((scope) => expect(scope.isDone()).toBeFalsy());
    });

    test('should get shop opinions', async () => {
        const mockScopes = raiseMocks(shopsOpinionsMock);
        const shopId = { shopId: 720 };
        const response = await API.yandex.market['v2.1.5'].shops.opinions(shopId);
        expect(response).toEqual('OK');
        mockScopes.forEach((scope) => scope.done());
    });

    test('should get parser error', async () => {
        const mockScopes = raiseMocks(shopsOpinionsMockError);
        const shopIdError = { shopId: 123 };
        let response;
        let errorCatched;
        try {
            response = await API.yandex.market['v2.1.5'].shops.opinions(shopIdError);
        } catch (error) {
            errorCatched = true;
        }
        expect(errorCatched).toBeTruthy();
        expect(response).toBeUndefined();
        mockScopes.forEach((scope) => scope.done());
    });

    test('should get invalid response', async () => {
        const mockScopes = raiseMocks(shopsMockInvalid);
        const shopIdInvalid = { shopId: 720 };
        let response;
        let errorCatched;
        try {
            response = await API.yandex.market['v2.1.5'].shops.opinions(shopIdInvalid);
        } catch (error) {
            errorCatched = true;
        }
        expect(errorCatched).toBeTruthy();
        expect(response).toBeUndefined();
        mockScopes.forEach((scope) => scope.done());
    });
});
