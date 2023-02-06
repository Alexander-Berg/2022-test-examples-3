import { setupServer } from 'msw/node';
import { rest } from 'msw';
import { requestAuth } from './utils';

const server = setupServer(
    rest.post('http://crm.backend/proxy/handle/200', (req, res, ctx) => {
        return res(ctx.status(200), ctx.set('Header', 'Value'), ctx.json({ data: 'some data' }));
    }),

    rest.post('http://crm.backend/proxy/handle/403', (req, res, ctx) => {
        return res(ctx.status(403), ctx.set('Header', 'Value'), ctx.json({ error: 'some error' }));
    }),
);

beforeAll(() => server.listen({ onUnhandledRequest: 'bypass' }));
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

describe('proxy feature', function() {
    it('proxies 200', async() => {
        const response = await requestAuth.post('/proxy/handle/200');

        expect(response.status).toEqual(200);
        expect(response.headers.header).toEqual('Value');
        expect(response.body).toStrictEqual({ data: 'some data' });
    });

    it('proxies 403', async() => {
        const response = await requestAuth.post('/proxy/handle/403');

        expect(response.status).toEqual(403);
        expect(response.headers.header).toEqual('Value');
        expect(response.body).toStrictEqual({ error: 'some error' });
    });
});
