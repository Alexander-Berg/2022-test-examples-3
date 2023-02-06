'use strict';

const sinon = require('sinon');

const lib = require('./../../../../../lib');
const routes = require('./../../../../../../routes');
const logs = require('./../../../../../../utils/logs');
const redir = require('./../../../../../../redir');

describe.skip('sovetnik / product / search / model / by text', () => {
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

    test('should return expected result', async () => {
        const client = new lib.Client(lib.stub.settings.partner.sovetnik, lib.stub.settings.user.sovetnik);

        const response = await client.request(
            routes.products,
            lib.stub.requests.product['Sovetnik | Monopoly | eldorado.ru'],
            lib.mock.API.yandex.market['v1.0.0'].category.match['Monopoly'],
            lib.mock.API.yandex.market['v2.1.0'].search['Monopoly | empty1'],
            lib.mock.API.yandex.market['v2.1.0'].search['Monopoly | empty2'],
            lib.mock.API.yandex.market['v2.0.0'].models.match['Monopoly | empty'],
            lib.mock.API.yandex.market['v2.1.0'].search['Monopoly | eldorado.ru'],
            lib.mock.API.yandex.market['v1.0.0'].model.outlets['Monopoly'],
            lib.mock.API.yandex.market['v2.1.0'].models.offers['Monopoly'],
        );

        const actual = response.response;
        actual.bucketInfo = {}; // FIXME: find better decision
        delete actual.settings.needShowGDPR;
        const logs = response.logs.product;
        const expected = lib.stub.responses.product['Sovetnik | Monopoly | eldorado.ru'];

        expect(logs).toHaveProperty('search_type', 'text');
        expect(actual).toEqual(expected);
    });
});
