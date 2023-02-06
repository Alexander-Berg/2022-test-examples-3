import nock from 'nock';

import API from '../../../../../src/API/index';
import raiseMocks from '../../../helpers/raise-mocks';
import { Opinion } from '../../../../../src/types/opinion';

/**
 * @see {@link https://jestjs.io/docs/en/manual-mocks#mocking-user-modules}
 */

if (!process.env.LOG) {
    jest.mock('../../../../../utils/logs');
}

/* Mocks */
const opinionOk = require('./__mocks__/model-opinions-ok');
const opinionInvalid = require('./__mocks__/model-opinions-invalid');

/* Stubs */
const opinionsStub = require('./__stubs__/api-models-opinions.stub');

describe('Model Opinions API', () => {
    afterEach(() => {
        nock.cleanAll();
    });

    test('should fail validation (no params)', async () => {
        const mockScopes = raiseMocks(opinionOk);

        let response: Array<Opinion>;
        try {
            // eslint-disable-next-line @typescript-eslint/ban-ts-ignore
            // @ts-ignore
            response = await API.yandex.market.pers.pstatic({});
        } catch (error) {
            response = [];
        }
        expect(response).toEqual([]);
        mockScopes.forEach((scope) => expect(scope.isDone()).toBeFalsy());
    });

    test('should fail validation (no regionId)', async () => {
        const mockScopes = raiseMocks(opinionOk);

        let response: Array<Opinion>;
        try {
            // eslint-disable-next-line @typescript-eslint/ban-ts-ignore
            // @ts-ignore
            response = await API.yandex.market.pers.pstatic({
                modelId: 54321,
                reqId: '123214434',
            });
        } catch (error) {
            response = [];
        }

        expect(response).toEqual([]);
        mockScopes.forEach((scope) => expect(scope.isDone()).toBeFalsy());
    });

    test('should fail with invalid response', async () => {
        const mockScopes = raiseMocks(opinionInvalid);

        let error;

        try {
            await API.yandex.market.pers.pstatic({
                modelId: 54321,
                regionId: 123,
                credentials: '',
                remoteIp: '',
                reqId: '23432423434',
            });
        } catch (e) {
            error = e;
        }

        expect(error).toBeTruthy();
        mockScopes.forEach((scope) => scope.done());
    });

    test('should find model by text search', async () => {
        const mockScopes = raiseMocks(opinionOk);
        const response = await API.yandex.market.pers.pstatic({
            modelId: 54321,
            regionId: 123,
            credentials: '',
            remoteIp: '',
            reqId: '2343545345',
        });

        expect(response).toEqual(opinionsStub);
        mockScopes.forEach((scope) => scope.done());
    });
});
