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
const modelsOk = require('./__mocks__/models-search-ok');
const modelsFail = require('./__mocks__/models-search-fail');
const modelError = require('./__mocks__/models-search-error');

/* Stubs */
const modelsStub = require('./__stubs__/api-models.stub');

describe('Models API', () => {
    afterEach(() => {
        nock.cleanAll();
    });

    test('should fail validation', async () => {
        const mockScopes = raiseMocks(modelsOk);
        let response;
        let errorCatched;
        try {
            response = await API.yandex.market['v2.1.4'].models.getMultipleModelsInfo({});
        } catch (e) {
            errorCatched = true;
        }
        expect(errorCatched).toBeTruthy();
        expect(response).toBeUndefined();
        mockScopes.forEach((scope) => expect(scope.isDone()).toBeFalsy());
    });

    test('should find model by id', async () => {
        const modelId = {
            modelIds: 546408366,
        };

        const mockScopes = raiseMocks(modelsOk);
        const response = await API.yandex.market['v2.1.4'].models.getMultipleModelsInfo(modelId);
        expect(response).toEqual(modelsStub);

        mockScopes.forEach((scope) => scope.done());
    });

    test('should fail ', async () => {
        const modelIdFail = {
            modelIds: 100500,
        };
        const mockScopes = raiseMocks(modelsFail);
        let response;
        let errorCatched;
        try {
            response = await API.yandex.market['v2.1.4'].models.getMultipleModelsInfo(modelIdFail);
        } catch (e) {
            errorCatched = true;
        }
        expect(errorCatched).toBeTruthy();
        expect(response).toBeUndefined();
        mockScopes.forEach((scope) => scope.done());
    });

    test('should not find model by id', async () => {
        const modelIdError = {
            modelIds: 100500,
        };
        const mockScopes = raiseMocks(modelError);

        let response;
        let modelIdFindError;
        try {
            response = await API.yandex.market['v2.1.4'].models.getMultipleModelsInfo(modelIdError);
        } catch (e) {
            modelIdFindError = true;
        }
        expect(modelIdFindError).toBeTruthy();
        expect(response).toBeUndefined();

        mockScopes.forEach((scope) => scope.done());
    });
});
