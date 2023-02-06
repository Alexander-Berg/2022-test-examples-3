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
const shopsMock = require('./__mocks__/shops-info');
const shopsMockError = require('./__mocks__/shops-info-error');
const shopsMockInvalid = require('./__mocks__/shops-info-invalid');

/* Stubs */
const responseStub = require('./__stubs__/shops.stub');

describe('Get shops', () => {
    afterEach(() => {
        nock.cleanAll();
    });

    test('should fail validation', async () => {
        const mockScopes = raiseMocks(shopsMock);
        let response;
        let errorCatched;
        try {
            response = await API.yandex.market['v2.1.5'].shops.info({});
        } catch (error) {
            errorCatched = true;
        }
        expect(errorCatched).toBeTruthy();
        expect(response).toBeUndefined();
        mockScopes.forEach((scope) => expect(scope.isDone()).toBeFalsy());
    });

    test('should get shop info', async () => {
        const mockScopes = raiseMocks(shopsMock);
        const shopId = { shopId: 720 };
        const response = await API.yandex.market['v2.1.5'].shops.info(shopId);
        expect(response).toEqual(responseStub);
        mockScopes.forEach((scope) => scope.done());
    });

    test('should get parser error', async () => {
        const mockScopes = raiseMocks(shopsMockError);
        const shopIdError = { shopId: 123 };
        let response;
        let errorCatched;
        try {
            response = await API.yandex.market['v2.1.5'].shops.info(shopIdError);
        } catch (error) {
            errorCatched = true;
        }
        expect(errorCatched).toBeTruthy();
        expect(response).toBeUndefined();
        mockScopes.forEach((scope) => scope.done());
    });

    test('should get error - invalid response', async () => {
        const mockScopes = raiseMocks(shopsMockInvalid);
        const shopIdInvalid = { shopId: 720 };
        let response;
        let errorCatched;
        try {
            response = await API.yandex.market['v2.1.5'].shops.info(shopIdInvalid);
        } catch (error) {
            errorCatched = true;
        }
        expect(errorCatched).toBeTruthy();
        expect(response).toBeUndefined();
        mockScopes.forEach((scope) => scope.done());
    });
});
