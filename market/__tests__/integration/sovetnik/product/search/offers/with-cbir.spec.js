'use strict';

const sinon = require('sinon');

const lib = require('./../../../../../lib');
const routes = require('./../../../../../../routes');
const logs = require('./../../../../../../utils/logs');
const redir = require('./../../../../../../redir');

const expected = require('./with-cbir.stub');

//need to fix after SOVETNIK-13831
// TODO: rewrite
describe.skip('sovetnik / product / search / offers / with cbir', () => {
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
            lib.stub.requests.product['Sovetnik | Dress-Vero-Moda | wildberries.ru'],
            lib.mock.API.yandex.market['v1.0.0'].category.match['Dress-Vero-Moda'],
            lib.mock.API.yandex.images['v1.0.0'].cbir.market['Dress-Vero-Moda'],
            lib.mock.API.yandex.market['v2.1.4'].offers['Dress-Vero-Moda'],
        );

        const { response: actual } = response;
        actual.bucketInfo = {}; // TODO: find better decision
        delete actual.settings.needShowGDPR;

        expect(actual).toEqual(expected);
    });
});
