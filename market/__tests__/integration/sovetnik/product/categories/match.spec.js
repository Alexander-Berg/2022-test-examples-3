/* eslint-disable max-len */

'use strict';

const sinon = require('sinon');

const lib = require('../../../../lib/index');
const routes = require('../../../../../routes/index');
const logs = require('../../../../../utils/logs');
const redir = require('../../../../../redir/index');

const expected = require('./match.stub');

// TODO: rewrite
describe.skip('sovetnik / product / categories / match', () => {
    let redirStub;
    let logsStub;

    beforeAll(() => {
        redirStub = sinon.stub(redir, 'wrapUrl', () => '<wrapped url>');
        logsStub = {
            trackSettings: sinon.stub(logs, 'trackSettings', () => undefined),
            trackDomainData: sinon.stub(logs, 'trackDomainData', () => undefined),
            trackDisableReason: sinon.stub(logs, 'trackDisableReason', () => undefined),
            trackExternalApiCall: sinon.stub(logs, 'trackExternalApiCall', () => undefined),
        };
    });

    afterAll(() => {
        redirStub.restore();
        logsStub.trackSettings.restore();
        logsStub.trackDomainData.restore();
        logsStub.trackDisableReason.restore();
        logsStub.trackExternalApiCall.restore();
    });

    test('should return expected result with normal price', async () => {
        const client = new lib.Client(lib.stub.settings.partner.sovetnik, lib.stub.settings.user.sovetnik);

        const response = await client.request(
            routes.products,
            lib.stub.requests.product['Sovetnik | Apple iPhone X 256Gb | e-katalog.ru (2)'],
            lib.mock.API.yandex.market['v2.0.0'].categories.match['Apple iPhone X 256Gb | 200'],
            lib.mock.API.yandex.market['v1.0.0'].shops['e-katalog.ru | 200'],
            lib.mock.API.yandex.market['v2.1.0'].search['Apple iPhone X 256Gb | e-katalog.ru | 200 | (1)'],
            lib.mock.API.yandex.market['v2.1.0'].search['Apple iPhone X 256Gb | e-katalog.ru | 200 | (2)'],
            lib.mock.API.yandex.market['v1.0.0'].model.match['Apple iPhone X 256Gb | 200'],
            lib.mock.API.yandex.market['v2.1.4'].model['Apple iPhone X 256Gb | 200'],
            lib.mock.API.yandex.market['v1.0.0'].model.outlets['Apple iPhone X 256Gb | 200'],
            lib.mock.API.yandex.market['v2.1.0'].models.offers['Apple-iPhone-X-256GB'],
        );

        const actual = response.response;
        actual.bucketInfo = {}; // TODO: find better decision
        delete actual.settings.needShowGDPR;

        expect(actual).toEqual(expected);
    });
});
