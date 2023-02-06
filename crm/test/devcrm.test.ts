import { setupServer } from 'msw/node';
import { rest } from 'msw';
import { requestAuth, AUTH_COOKIE } from './utils';

const server = setupServer();

beforeAll(() => server.listen({ onUnhandledRequest: 'bypass' }));
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

describe('devcrm cookie feature', function() {
    describe('when backend client request', () => {
        it('sends x-crm-dev-login', async() => {
            let request = null;

            server.use(
                rest.post('http://crm.backend/auth/logout/asdelegated', (req, res, ctx) => {
                    request = req;
                    return res(ctx.status(200));
                }),
            );

            await requestAuth.post('/auth/logout/asdelegated').set('Cookie', `devcrm=crm-login;${AUTH_COOKIE}`);
            expect(request.headers.get('x-crm-dev-login')).toEqual('crm-login');
        });
    });

    describe('when backend proxy request', () => {
        it('sends x-crm-dev-login', async() => {
            let request = null;

            server.use(
                rest.get('http://crm.backend/some/proxy/handler', (req, res, ctx) => {
                    request = req;
                    return res(ctx.status(200));
                }),
            );

            await requestAuth.get('/some/proxy/handler').set('Cookie', `devcrm=crm-login;${AUTH_COOKIE}`);
            expect(request.headers.get('x-crm-dev-login')).toEqual('crm-login');
        });
    });
});
