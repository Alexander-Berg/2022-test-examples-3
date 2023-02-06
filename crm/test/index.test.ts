import { setupServer } from 'msw/node';
import { rest } from 'msw';
import { requestAuth } from './utils';

const server = setupServer();

beforeAll(() => server.listen({ onUnhandledRequest: 'bypass' }));
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

describe('GET /', function() {
    describe('when info request success', () => {
        it('get index with data', async function() {
            const infoData = { key: 'value' };

            server.use(
                rest.get('http://crm.backend/info', (req, res, ctx) => {
                    return res(ctx.json(infoData));
                }),
            );

            const response = await requestAuth.get('/');

            const [nonce, dataStr] = response.text.split('\n');
            const data = JSON.parse(dataStr);

            expect(response.status).toEqual(200);
            expect(data.nonce).toEqual(nonce);
            expect(data.info).toEqual(infoData);
            expect(data.error).toEqual(null);
        });
    });

    describe('when info request fail', () => {
        it('get index with error data', async function() {
            server.use(
                rest.get('http://crm.backend/info', (req, res, ctx) => {
                    return res(ctx.status(500), ctx.json({ message: 'error' }));
                }),
            );

            const response = await requestAuth.get('/');

            const [nonce, dataStr] = response.text.split('\n');
            const data = JSON.parse(dataStr);

            expect(response.status).toEqual(200);
            expect(data.nonce).toEqual(nonce);
            expect(data.info).toEqual({});
            expect(data.error).toEqual({ name: 'CRMBackendRequestError', message: 'error (500)', code: 'ERR_NON_2XX_3XX_RESPONSE' });
        });
    });

    describe('when info request fail by delegation', () => {
        it('get index with error data', async function() {
            server.use(
                rest.get('http://crm.backend/info', (req, res, ctx) => {
                    return res(ctx.status(403), ctx.json({ message: 'error', code: 'delegation-error' }));
                }),
            );

            const response = await requestAuth.get('/');

            const [nonce, dataStr] = response.text.split('\n');
            const data = JSON.parse(dataStr);

            expect(response.status).toEqual(200);
            expect(data.nonce).toEqual(nonce);
            expect(data.info).toEqual({});
            expect(String(response.headers['set-cookie'])).toMatch(/delegationId=;/);
            expect(data.error).toEqual({ name: 'CRMBackendRequestError', message: 'error (403)', code: 'delegation-error' });
        });
    });
});

describe('GET /index.html', function() {
    it('redirects to /', function(done) {
        requestAuth
            .get('/index.html')
            .expect('Location', '/')
            .expect(302, done);
    });
});

describe('GET /space/index.html', function() {
    it('redirects to /', function(done) {
        requestAuth
            .get('/space/index.html')
            .expect('Location', '/')
            .expect(302, done);
    });
});
