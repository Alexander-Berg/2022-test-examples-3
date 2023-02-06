'use strict';

const sinon = require('sinon');

const lib = require('./../../../../lib');
const routes = require('./../../../../../routes');
const logs = require('./../../../../../utils/logs');
const redir = require('./../../../../../redir');
const expectedLg = require('./native.lg.stub');

// TODO: rewrite
describe.skip('sovetnik / settings / disable', () => {
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

    test('should not return offers for anybody if native sovetnik was disabled', async () => {
        const client = new lib.Client(lib.stub.settings.partner.yabro, lib.stub.settings.user.yabro);

        // Send request from Yandex Browser to our backend
        let response = await client.request(
            routes.products,
            lib.stub.requests.product['YaBro | LG 43LJ510V | eldorado.ru'],
            lib.mock.API.yandex.market['v1.0.0'].category.match['LED TV LG 43LJ510V | 200'],
            lib.mock.API.yandex.market['v2.1.0'].search['LG 43LJ510V | eldorado.ru | 200 | (1)'],
            lib.mock.API.yandex.market['v2.1.0'].search['LG 43LJ510V | eldorado.ru | 200 | (2)'],
            lib.mock.API.yandex.market['v2.0.0'].models.match['LG-43LJ510v'],
            lib.mock.API.yandex.market['v2.1.4'].model['LG 43LJ510V | 200'],
            lib.mock.API.yandex.market['v1.0.0'].model.outlets['LG 43LJ510V | 200'],
            lib.mock.API.yandex.market['v2.1.0'].models.offers['LG-43LJ510V'],
        );

        {
            const actual = response.response;
            actual.bucketInfo = {}; // TODO: find better decision

            expect(actual).toEqual(expectedLg);
        }

        // Send request which will disable native script
        response = await client.request(routes.sovetnikDisabled, lib.stub.requests.disable.YaBro);

        // Set SaveFrom settings
        client.applySettings(lib.stub.settings.partner.saveFrom, response.getCookieValue('svt-user'));

        // Send request from Yandex Browser to our backend to be sure that Sovetnik is disabled
        response = await client.request(
            routes.products,
            lib.stub.requests.product['SaveFrom | LG 43LJ510V | eldorado.ru'],
            lib.mock.API.yandex.market['v1.0.0'].category.match['LED TV LG 43LJ510V | 200'],
            lib.mock.API.yandex.market['v2.1.0'].search['LG 43LJ510V | eldorado.ru | 200 | (1)'],
            lib.mock.API.yandex.market['v2.1.0'].search['LG 43LJ510V | eldorado.ru | 200 | (2)'],
            lib.mock.API.yandex.market['v2.0.0'].models.match['LG-43LJ510v'],
            lib.mock.API.yandex.market['v2.1.4'].model['LG 43LJ510V | 200'],
            lib.mock.API.yandex.market['v1.0.0'].model.outlets['LG 43LJ510V | 200'],
            lib.mock.API.yandex.market['v2.1.0'].models.offers['LG-43LJ510V'],
        );

        {
            const actual = response.response;
            actual.bucketInfo = {}; // FIXME: find better decision
            delete actual.settings.needShowGDPR; // fixme
            const expected = lib.stub.responses.product['SaveFrom (disable) | LG-43LJ510V | eldorado.ru'];

            expect(actual).toEqual(expected);
        }

        {
            const actual = response.logs.product;

            expect(actual).toHaveProperty('do_not_search', 1);
            expect(actual).toHaveProperty('do_not_search_reason', 'ya_bro_disabled');
        }
    });
});
