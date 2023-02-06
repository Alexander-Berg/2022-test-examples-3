'use strict';

const sinon = require('sinon');

const lib = require('./../../../../../lib');
const routes = require('./../../../../../../routes');
const logs = require('./../../../../../../utils/logs');
const redir = require('./../../../../../../redir');
const expectedIphoneSE = require('./by-url.iphone-se.stub');
const expectedIphoneX = require('./by-url.iphone-x.stub');

// TODO: rewrite
describe.skip('sovetnik / product / search / model / by url', () => {
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

    test('should return expected result if searching by isbn returns empty result', async () => {
        const client = new lib.Client(lib.stub.settings.partner.sovetnik, lib.stub.settings.user.sovetnik);

        const response = await client.request(
            routes.products,
            lib.stub.requests.product['Sovetnik | Apple iPhone X 256Gb | e-katalog.ru'],
            lib.mock.API.yandex.market['v1.0.0'].category.match['Apple iPhone X 256Gb | 200'],
            lib.mock.API.yandex.market['v2.1.0'].search['Apple iPhone X 256Gb | e-katalog.ru | 200 | (1)'],
            lib.mock.API.yandex.market['v2.1.0'].search['Apple iPhone X 256Gb | e-katalog.ru | 200 | (2)'],
            lib.mock.API.yandex.market['v2.0.0'].models.match['Apple-iPhone-X-256Gb'],
            lib.mock.API.yandex.market['v2.1.4'].model['Apple iPhone X 256Gb | 200'],
            lib.mock.API.yandex.market['v1.0.0'].model.outlets['Apple iPhone X 256Gb | 200'],
            lib.mock.API.yandex.market['v2.1.0'].models.offers['Apple-iPhone-X-256GB'],
        );

        const actual = response.response;
        actual.bucketInfo = {}; // TODO: find better decision
        delete actual.settings.needShowGDPR; // fixme

        expect(actual).toEqual(expectedIphoneX);
    });

    test('should return expected result if search by url returns full result', async () => {
        const client = new lib.Client(lib.stub.settings.partner.sovetnik, lib.stub.settings.user.sovetnik);

        const response = await client.request(
            routes.products,
            lib.stub.requests.product['Sovetnik | Apple iPhone SE 64Gb Silver A1662 | cultgoods.com'],
            lib.mock.API.yandex.market['v1.0.0'].category.match['Apple iPhone SE 64Gb Silver A1662 | 200'],
            lib.mock.API.yandex.market['v2.1.0'].search[
                'Apple iPhone SE 64Gb Silver A1662 | cultgoods.com | 200 | (1)'
            ],
            lib.mock.API.yandex.market['v2.1.0'].search[
                'Apple iPhone SE 64Gb Silver A1662 | cultgoods.com | 200 | (2)'
            ],
            lib.mock.API.yandex.market['v2.1.4'].model['Apple iPhone SE 64Gb Silver A1662 | 200'],
            lib.mock.API.yandex.market['v1.0.0'].model.outlets['Apple iPhone SE 64Gb Silver A1662 | 200'],
            lib.mock.API.yandex.market['v2.1.0'].models.offers['Apple-iPhone-SE-64GB'],
        );

        const actual = response.response;
        actual.bucketInfo = {}; // TODO: find better decision
        delete actual.settings.needShowGDPR;

        expect(actual).toEqual(expectedIphoneSE);
    });
});
