'use strict';

const sinon = require('sinon');

const lib = require('./../../../../../lib');
const routes = require('./../../../../../../routes');
const logs = require('./../../../../../../utils/logs');
const redir = require('./../../../../../../redir');

describe.skip('sovetnik / product / search / offers / by text', () => {
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

    it('should return expected result if search by text returns full result', async () => {
        const client = new lib.Client(lib.stub.settings.partner.sovetnik, lib.stub.settings.user.sovetnik);

        const response = await client.request(
            routes.products,
            lib.stub.requests.product['Sovetnik | Letto | Wildberries.ru'],
            lib.mock.API.yandex.market['v1.0.0'].category.match['Letto | 200'],
            lib.mock.API.yandex.market['v2.1.0'].search['Letto | wildberries.ru | 200 | (1)'],
            lib.mock.API.yandex.market['v2.1.0'].search['Letto | wildberries.ru | 200 | (2)'],
            lib.mock.API.yandex.market['v1.0.0'].model.match['Letto | 200'],
            lib.mock.API.yandex.market['v1.0.0'].category['Letto | 200'],
            lib.mock.API.yandex.market['v1.0.0'].search['Letto | wildberries.ru | 200'],
            lib.mock.API.yandex.market['v2.1.0'].search['Letto | wildberries.ru | 200 | (4)'],
        );

        const { response: actual } = response;
        actual.bucketInfo = {}; // TODO: find better decision
        delete actual.settings.needShowGDPR;
        const expected = lib.stub.responses.product['Sovetnik | Letto | wildberries.ru'];

        expect(actual).toEqual(expected);
    });
});
