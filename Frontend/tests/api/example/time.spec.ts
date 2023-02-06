import { Server } from 'http';
import nock from 'nock';
import request from 'supertest';
import sinon, { SinonStub } from 'sinon';

import configureServer from 'src/server/tests/helpers/server';
import * as nocks from 'src/server/tests/nocks';

describe('GET /api/example/time', () => {
    let server: Server;

    const currentTime = 1592686679317;

    beforeEach(async() => {
        server = await configureServer(8080);

        nocks.nockBlackbox({
            response: {
                status: {
                    value: 'VALID',
                },
            },
            ticket: 'some-ticket',
        });
        nocks.nockGeobase();
        nocks.nockLangdetect({});
        nocks.nockUatraits({});
        nocks.nockTvm({
            ticket: 'some-ticket',
        });

        sinon.stub(Date, 'now').returns(currentTime);
    });

    afterEach(() => {
        server.close();

        nock.cleanAll();

        (Date.now as SinonStub).restore();
    });

    it('should return correct Time', async() => {
        await request(server)
            .get('/api/example/time')
            .expect(200)
            .expect('content-type', 'application/json; charset=utf-8')
            .expect({ time: currentTime });
    });
});
