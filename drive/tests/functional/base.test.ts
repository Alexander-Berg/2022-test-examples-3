import * as http from 'http';
import * as got from 'got';
import {expect} from 'chai';
import {app} from 'app/app';
import {startServer, stopServer} from './test-server';

describe('the basic interface of the HTTP API', () => {
    let server: http.Server;
    let url: string;

    before(async () => {
        [server, url] = await startServer(app);
    });

    after(async () => {
        await stopServer(server);
    });

    it('should respond with 200 on /ping requests', async () => {
        const res = await got(`${url}/ping`);
        expect(res.statusCode).to.equal(200);
    });

    it('should respond with 404 on unsupported paths', async () => {
        const res = await got(`${url}/cat_pictures`, {throwHttpErrors: false});
        expect(res.statusCode).to.equal(404);
    });

    it('should be free of X-Powered-By headers', async () => {
        const res = await got(`${url}/ping`);
        expect(res.headers['x-powered-by'], 'X-Powered-By header should be disabled').to.not.exist;
    });
});
