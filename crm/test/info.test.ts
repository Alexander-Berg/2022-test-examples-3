import { setupServer } from 'msw/node';
import { rest } from 'msw';
import { requestAuth } from './utils';

const infoData = { key: 'value' };

const server = setupServer(
    rest.get('http://crm.backend/info', (req, res, ctx) => {
        return res(ctx.json(infoData));
    }),
);

beforeAll(() => server.listen({ onUnhandledRequest: 'bypass' }));
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

describe('GET /info', function() {
    describe('when proxy success', () => {
        it('returns patched info', async function() {
            const response = await requestAuth.get('/info');

            expect(response.status).toEqual(200);
            expect(response.body).toEqual(infoData);
        });
    });

    describe('when proxy fail', () => {
        it('returns error', async function() {
            server.use(
                rest.get('http://crm.backend/info', (req, res, ctx) => {
                    return res(ctx.status(500), ctx.json({ message: 'error' }));
                }),
            );

            const response = await requestAuth.get('/info');

            expect(response.status).toEqual(502);
            expect(response.body).toEqual({ message: 'error (500)' });
        });
    });

    describe('when proxy returns empty result', () => {
        it('returns patched info', async function() {
            server.use(
                rest.get('http://crm.backend/info', (req, res, ctx) => {
                    return res(ctx.status(200));
                }),
            );

            const response = await requestAuth.get('/info');

            expect(response.status).toEqual(200);
            expect(response.body).toEqual({});
        });
    });
});
