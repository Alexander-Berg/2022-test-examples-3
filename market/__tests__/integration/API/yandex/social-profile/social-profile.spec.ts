import nock from 'nock';

import API from '../../../../../src/API/index';
import raiseMocks from '../../../helpers/raise-mocks';
import * as tvm from '../../../../../src/API/yandex/tvm';

jest.mock('../../../../../src/API/yandex/tvm');

interface TvmMock {
    ticket: () => Promise<string>;
}
const tvmMock = tvm as jest.Mocked<TvmMock>;
tvmMock.ticket.mockImplementation(() => new Promise((resolve) => resolve('ticket')));

/**
 * @see {@link https://jestjs.io/docs/en/manual-mocks#mocking-user-modules}
 */

if (!process.env.LOG) {
    jest.mock('../../../../../utils/logs');
}

/* Mocks */
const socialProfileOk = require('./__mocks__/social-profile-ok');
const socialProfileInvalid = require('./__mocks__/social-profile-invalid');

/* Stubs */
const socialProfileStub = require('./__stubs__/social-profile.stub');

describe('Social profile API', () => {
    afterEach(() => {
        nock.cleanAll();
    });

    test('should fail validation (no params)', async () => {
        const mockScopes = raiseMocks(socialProfileOk);
        let error;
        try {
            // eslint-disable-next-line @typescript-eslint/ban-ts-ignore
            // @ts-ignore
            await API.yandex.social.profile({});
        } catch (err) {
            error = err;
        }

        expect(error).toBeTruthy();
        mockScopes.forEach((scope) => expect(scope.isDone()).toBeFalsy());
    });

    test('should fail validation (uids is empty)', async () => {
        const mockScopes = raiseMocks(socialProfileOk);

        let error;
        try {
            // eslint-disable-next-line @typescript-eslint/ban-ts-ignore
            // @ts-ignore
            await API.yandex.social.profile({
                uids: [],
                reqId: '12234324234',
            });
        } catch (err) {
            error = err;
        }

        expect(error).toBeTruthy();
        mockScopes.forEach((scope) => expect(scope.isDone()).toBeFalsy());
    });

    test('should fail with invalid response', async () => {
        const mockScopes = raiseMocks(socialProfileInvalid);
        let error;
        try {
            // eslint-disable-next-line @typescript-eslint/ban-ts-ignore
            // @ts-ignore
            await API.yandex.social.profile({
                uids: [321, 322, 323],
                reqId: '353453535',
            });
        } catch (err) {
            error = err;
        }

        expect(error).toBeTruthy();
        mockScopes.forEach((scope) => scope.done());
    });

    test('should get profiles by uids', async () => {
        const mockScopes = raiseMocks(socialProfileOk);
        const response = await API.yandex.social.profile({
            uids: [321, 322, 323],
            reqId: '454354534',
        });
        expect(response).toEqual(socialProfileStub);
        mockScopes.forEach((scope) => scope.done());
    });
});
