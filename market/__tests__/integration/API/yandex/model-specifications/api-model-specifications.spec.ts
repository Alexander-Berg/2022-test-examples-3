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
const specificationOk = require('./__mocks__/model-specifications-ok');
const specificationError = require('./__mocks__/model-specifications-error');
const specificationInvalid = require('./__mocks__/model-specifications-invalid');

/* Stubs */
const specificationsStub = require('./__stubs__/api-models-specifications.stub');

describe('Model Specifications API', () => {
    afterEach(() => {
        nock.cleanAll();
    });

    test('should fail validation (no params)', async () => {
        const mockScopes = raiseMocks(specificationOk);
        let response;
        try {
            response = await API.yandex.market['v2.1.5'].models.specification();
        } catch (error) {
            response = null;
        }
        expect(response).toBeNull();
        mockScopes.forEach((scope) => expect(scope.isDone()).toBeFalsy());
    });

    test('should fail validation (no geo_id)', async () => {
        const modelIdFailValidation: any = {
            modelId: 175941311,
        };
        const mockScopes = raiseMocks(specificationOk);
        let response;
        try {
            response = await API.yandex.market['v2.1.5'].models.specification(modelIdFailValidation);
        } catch (error) {
            response = null;
        }
        expect(response).toBeNull();
        mockScopes.forEach((scope) => expect(scope.isDone()).toBeFalsy());
    });

    test('should fail with error status', async () => {
        const modelIdError: any = {
            modelId: 175941311,
            geo_id: 213,
        };
        const mockScopes = raiseMocks(specificationError);
        let response;
        try {
            response = await API.yandex.market['v2.1.5'].models.specification(modelIdError);
        } catch (error) {
            response = null;
        }
        expect(response).toBeNull();
        mockScopes.forEach((scope) => scope.done());
    });

    test('should fail with invalid status', async () => {
        const modelIdInvalid: any = {
            modelId: 175941311,
            geo_id: 213,
        };
        const mockScopes = raiseMocks(specificationInvalid);
        let response;
        try {
            response = await API.yandex.market['v2.1.5'].models.specification(modelIdInvalid);
        } catch (error) {
            response = null;
        }
        expect(response).toBeNull();
        mockScopes.forEach((scope) => expect(scope.isDone()));
    });

    test('should find model specifications', async () => {
        const modelId: any = {
            modelId: 175941311,
            geo_id: 213,
        };
        const mockScopes = raiseMocks(specificationOk);
        const response = await API.yandex.market['v2.1.5'].models.specification(modelId);
        expect(response).toEqual(specificationsStub);
        mockScopes.forEach((scope) => scope.done());
    });
});
