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
const serarchOk = require('./__mocks__/search-ok');
const serarchError = require('./__mocks__/search-status-error');
const serarchInvalid = require('./__mocks__/search-status-invalid');

/* Stubs */
const searchStub = require('./__stubs__/api-search.stub');

describe('Search API', () => {
    afterEach(() => {
        nock.cleanAll();
    });

    test('should fail validation (no params)', async () => {
        const mockScopes = raiseMocks(serarchOk);
        let response;
        let errorCatched;
        try {
            response = await API.yandex.market['v2.0.0'].search([{}]);
        } catch (error) {
            errorCatched = true;
        }
        expect(errorCatched).toBeTruthy();
        expect(response).toBeUndefined();
        mockScopes.forEach((scope) => expect(scope.isDone()).toBeFalsy());
    });

    test('should fail validation (no geo_id)', async () => {
        const searchId: any = {
            text: 'iPhone',
        };
        const mockScopes = raiseMocks(serarchOk);
        let response;
        let errorCatched;
        try {
            response = await API.yandex.market['v2.0.0'].search(searchId);
        } catch (error) {
            errorCatched = true;
        }
        expect(errorCatched).toBeTruthy();
        expect(response).toBeUndefined();
        mockScopes.forEach((scope) => expect(scope.isDone()).toBeFalsy());
    });

    test('should fail with error status', async () => {
        const searchIdError: any = {
            text: 'iPhone',
            geo_id: 213,
        };
        const mockScopes = raiseMocks(serarchError);
        let response;
        let errorCatched;
        try {
            response = await API.yandex.market['v2.0.0'].search(searchIdError);
        } catch (error) {
            errorCatched = true;
        }
        expect(errorCatched).toBeTruthy();
        expect(response).toBeUndefined();
        mockScopes.forEach((scope) => scope.done());
    });

    test('should fail with invalid status', async () => {
        const searchIdInvalid: any = {
            text: 'iPhone',
            geo_id: 213,
        };
        const mockScopes = raiseMocks(serarchInvalid);
        let response;
        let errorCatched;
        try {
            response = await API.yandex.market['v2.0.0'].search(searchIdInvalid);
        } catch (error) {
            errorCatched = true;
        }
        expect(errorCatched).toBeTruthy();
        expect(response).toBeUndefined();
        mockScopes.forEach((scope) => scope.done());
    });

    test('should find model by text search', async () => {
        const searchId: any = {
            text: 'iPhone',
            geo_id: 213,
        };
        const mockScopes = raiseMocks(serarchOk);
        let response;
        let errorCatched;
        try {
            response = await API.yandex.market['v2.0.0'].search(searchId);
        } catch (error) {
            errorCatched = true;
        }
        expect(errorCatched).toBeFalsy();
        expect(response).toEqual(searchStub);
        mockScopes.forEach((scope) => scope.done());
    });
});
