import nock from 'nock';

import API from '../../../../../src/API/index';
import raiseMocks from '../../../helpers/raise-mocks';
import { AdFoxBanner, AdFoxInfo, AdFoxRequestParams } from '../../../../../src/types/adfox-params';
import { SOVETNIK_ADFOX_ACCOUT_ID } from '../../../../../src/constants';

/**
 * @see {@link https://jestjs.io/docs/en/manual-mocks#mocking-user-modules}
 */
if (!process.env.LOG) {
    jest.mock('../../../../../utils/logs');
}

/* Mocks */
const adfoxOk = require('./__mocks__/adfox-ok');
const adfoxNok = require('./__mocks__/adfox-nok');

/* Stubs */
const adFoxStub = require('./__stubs__/adfox.stub');

const params = {
    reqid: '123456',
    ip: '127.0.0.1',
    userAgent: 'Chrome',
    referer: 'market.yandex.ru',
    data: [
        {
            id: '0',
            parameters: {
                puid11: '213',
                pp: 'g',
                p2: 'fuvz',
                ps: 'cmou',
                utf8: '✓',
            },
            owner_id: `${SOVETNIK_ADFOX_ACCOUT_ID}`,
        },
    ],
};

describe('Test AdFox getBukl Banners Api', () => {
    afterEach(() => {
        nock.cleanAll();
    });

    test('should fail params validation', async () => {
        const mockScopes = raiseMocks(adfoxOk);

        let response;
        let errorCatched;
        try {
            response = await API.yandex.adfox.v1.getBulk({} as AdFoxRequestParams);
        } catch (error) {
            errorCatched = true;
        }

        expect(errorCatched).toBeTruthy();
        expect(response).toBeUndefined();
        mockScopes.forEach((scope) => expect(scope.isDone()).toBeFalsy());
    });

    test('should handle parser error', async () => {
        const mockScopes = raiseMocks(adfoxNok);
        let adFoxResponse: AdFoxInfo = { banners: [], errors: [] };
        let adFoxBanners: Array<AdFoxBanner> = [];
        try {
            adFoxResponse = await API.yandex.adfox.v1.getBulk(params);
            adFoxBanners = adFoxResponse.banners;
        } catch (e) {
            expect(adFoxBanners).toEqual([]);
            expect(e.message).toContain('Parser errors');
        }

        mockScopes.forEach((scope) => scope.done());
    });

    test('should find banners', async () => {
        const mockScopes = raiseMocks(adfoxOk);

        const adFoxResponse: AdFoxInfo = await API.yandex.adfox.v1.getBulk(params);
        const { banners: adFoxBanners } = adFoxResponse;

        expect(adFoxBanners).toEqual(adFoxStub);
        mockScopes.forEach((scope) => scope.done());
    });
});
