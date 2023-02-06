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
const offersDefaultOk = require('./__mocks__/model-get-info-ok');
const offersDefaultError = require('./__mocks__/model-get-info-error');
const offersDefaultInvalid = require('./__mocks__/model-get-info-invalid');

/* Stubs */
const offersDefaultStub = require('./__stubs__/api-models-get-info.stub');

describe('Model Info API', () => {
    afterEach(() => {
        nock.cleanAll();
    });

    test('should fail validation (no params)', async () => {
        const mockScopes = raiseMocks(offersDefaultOk);
        let response;
        let errorCatched;
        try {
            response = await API.yandex.market['v2.1.4'].models.getModelInfo([{}]);
        } catch (ex) {
            errorCatched = true;
        }
        expect(errorCatched).toBeTruthy();
        expect(response).toBeUndefined();
        mockScopes.forEach((scope) => expect(scope.isDone()).toBeFalsy());
    });

    test('should fail with error status', async () => {
        const modelIdError: any = {
            modelId: 175941311,
            geo_id: 213,
        };
        const mockScopes = raiseMocks(offersDefaultError);
        let response;
        let errorCatched;
        try {
            response = await API.yandex.market['v2.1.4'].models.getModelInfo(modelIdError);
        } catch (ex) {
            errorCatched = true;
        }
        expect(errorCatched).toBeTruthy();
        expect(response).toBeUndefined();
        mockScopes.forEach((scope) => scope.done());
    });

    test('should fail with invalid status', async () => {
        const modelIdInvalid: any = {
            modelId: 175941311,
            geo_id: 213,
        };
        const mockScopes = raiseMocks(offersDefaultInvalid);
        let response;
        let errorCatched;
        try {
            response = await API.yandex.market['v2.1.4'].models.getModelInfo(modelIdInvalid);
        } catch (ex) {
            errorCatched = true;
        }
        expect(errorCatched).toBeTruthy();
        expect(response).toBeUndefined();
        mockScopes.forEach((scope) => scope.done());
    });

    test('should find model info', async () => {
        const modelId: any = {
            modelId: 175941311,
            geo_id: 213,
        };
        const mockScopes = raiseMocks(offersDefaultOk);
        let response;
        let errorCatched;
        try {
            response = await API.yandex.market['v2.1.4'].models.getModelInfo(modelId);
        } catch (ex) {
            errorCatched = true;
        }
        expect(errorCatched).toBeFalsy();
        expect(response).toEqual(offersDefaultStub);
        mockScopes.forEach((scope) => scope.done());
    });
});
