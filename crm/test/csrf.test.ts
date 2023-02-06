import { rest } from 'msw';
import { setupServer } from 'msw/node';
import { requestAuthWithoutCsrf, requestAuth } from './utils';

const server = setupServer(
    rest.post('http://crm.backend/some/handle', (req, res, ctx) => {
        return res(ctx.status(200));
    }),
);

beforeAll(() => server.listen({ onUnhandledRequest: 'bypass' }));
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

describe('csrf feature', () => {
    it('cleans old csrf cookie', async() => {
        const response = await requestAuth.post('/some/handle');

        expect(String(response.headers['set-cookie'])).toMatch(/x-csrf-token-frontend=;/);
    });

    describe('when POST request with csrf token header', () => {
        it('returns 200', async() => {
            const response = await requestAuth.post('/some/handle');

            expect(response.status).toEqual(200);
        });
    });

    describe('when POST request without csrf token', () => {
        it('returns 403', async() => {
            const response = await requestAuthWithoutCsrf.post('/some/handle');

            expect(response.status).toEqual(403);
            expect(response.body).toEqual({ code: 'EBADCSRFTOKEN', message: 'Invalid CSRF token' });
            expect(String(response.headers['set-cookie'])).toMatch(/x-csrf-token/);
        });
    });

    describe('when GET request without csrf token', () => {
        it('returns 200', async() => {
            const response = await requestAuthWithoutCsrf.get('/whoami');

            expect(response.status).toEqual(200);
        });
    });
});
