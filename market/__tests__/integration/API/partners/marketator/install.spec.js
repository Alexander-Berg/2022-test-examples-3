'use strict';

const sinon = require('sinon');

const lib = require('../../../../lib');
const logs = require('../../../../../utils/logs');
// const informPartners = require('../../../../../src/middlewares/partners/inform-partners-middleware');
const { initSettings } = require('../../../../../middleware/settings/settings');

/* Can not use `init-extension` middleware, because response doesn't contain isPartnerInformed */
const middlewareForTesting = [
    initSettings,
    // informPartners,
    (req, res) => {
        res.jsonp(req.isPartnerInformed);
    },
];

describe.skip('API / partners/ marketator', () => {
    let consoleErrorStub;
    let logsStub;

    beforeAll(() => {
        consoleErrorStub = sinon.stub(console, 'error', () => undefined);
        logsStub = sinon.stub(logs, 'trackPartnersExternalRequests', () => undefined);
    });

    afterAll(() => {
        consoleErrorStub.restore();
        logsStub.restore();
    });

    it('should perform call with clid and vid', async () => {
        const client = new lib.Client(lib.stub.settings.partner.sovetnik, lib.stub.settings.user.sovetnik);

        const response = await client.request(
            middlewareForTesting,
            lib.stub.requests.partners['Marketator | Clid | Vid | request'],
            lib.mock.API.partners.marketator.install['Marketator | Clid | Vid | 200'],
        );

        const { response: actual } = response;

        expect(actual).toBe(true);
    });

    it('should perform call without vid', async () => {
        const client = new lib.Client(lib.stub.settings.partner.sovetnik, lib.stub.settings.user.sovetnik);

        const response = await client.request(
            middlewareForTesting,
            lib.stub.requests.partners['Marketator | Clid | request'],
            lib.mock.API.partners.marketator.install['Marketator | Clid | 200'],
        );

        const { response: actual } = response;

        expect(actual).toBe(true);
    });
});
