import { setupServer } from 'msw/node';
import { rest } from 'msw';
import { requestAuth, AUTH_COOKIE } from './utils';

const server = setupServer(
    rest.post<{delegationId: number}>('http://crm.backend/auth/login/asdelegated', (req, res, ctx) => {
        if (!req.body.delegationId) {
            return res(ctx.status(400), ctx.json({ error: 'Has no delegationId' }));
        }

        if (req.body.delegationId === 100) {
            return res(ctx.status(400), ctx.json({ error: 'Not valid delegation' }));
        }

        return res(ctx.status(200));
    }),
);

beforeAll(() => server.listen({ onUnhandledRequest: 'bypass' }));
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

describe('delegation feature', function() {
    describe('POST auth/login/asdelegated', () => {
        describe('when wrong data scheme', () => {
            it('returns error', async function() {
                const response = await requestAuth.post('/auth/login/asdelegated').send({ data: 1 });

                expect(response.status).toEqual(400);
                expect(typeof response.body.message).toBe('string');
                expect(String(response.headers['set-cookie'])).not.toMatch(/delegationId=/);
            });
        });

        describe('when empty body', () => {
            it('returns error', async function() {
                const response = await requestAuth.post('/auth/login/asdelegated');

                expect(response.status).toEqual(400);
                expect(typeof response.body.message).toBe('string');
                expect(String(response.headers['set-cookie'])).not.toMatch(/delegationId=/);
            });
        });

        describe('when crm backend resposes success', () => {
            it('sets cookie', async function() {
                const response = await requestAuth.post('/auth/login/asdelegated').send({ delegationId: 1 });

                expect(response.status).toEqual(200);
                expect(String(response.headers['set-cookie'])).toMatch(/delegationId=1;/);
            });
        });

        describe('when crm backend resposes failed', () => {
            it('does not set cookie', async function() {
                const response = await requestAuth.post('/auth/login/asdelegated').send({ delegationId: 100 });

                expect(response.status).toEqual(502);
                expect(String(response.headers['set-cookie'])).not.toMatch(/delegationId=/);
            });
        });
    });

    describe('POST auth/logout/asdelegated', () => {
        describe('when crm backend resposes success', () => {
            it('removes cookie', async function() {
                server.use(
                    rest.post('http://crm.backend/auth/logout/asdelegated', (req, res, ctx) => {
                        return res(ctx.status(200));
                    }),
                );

                const response = await requestAuth.post('/auth/logout/asdelegated');

                expect(response.status).toEqual(200);
                expect(String(response.headers['set-cookie'])).toMatch(/delegationId=;/);
            });
        });

        describe('when crm backend resposes failed', () => {
            it('does not remove cookie', async function() {
                server.use(
                    rest.post('http://crm.backend/auth/logout/asdelegated', (req, res, ctx) => {
                        return res(ctx.status(500));
                    }),
                );

                const response = await requestAuth.post('/auth/logout/asdelegated');

                expect(response.status).toEqual(502);
                expect(String(response.headers['set-cookie'])).not.toMatch(/delegationId=/);
            });
        });
    });

    describe('when backend client request', () => {
        it('sends x-crm-delegation-id', async() => {
            let request = null;

            server.use(
                rest.post('http://crm.backend/auth/logout/asdelegated', (req, res, ctx) => {
                    request = req;
                    return res(ctx.status(200));
                }),
            );

            await requestAuth.post('/auth/logout/asdelegated').set('Cookie', `delegationId=1;${AUTH_COOKIE}`);
            expect(request.headers.get('x-crm-delegation-id')).toEqual('1');
        });

        describe('when delegation error', () => {
            it('removes cookie', async() => {
                server.use(
                    rest.post('http://crm.backend/auth/logout/asdelegated', (req, res, ctx) => {
                        return res(ctx.status(403), ctx.json({ code: 'delegation-error' }));
                    }),
                );

                const response = await requestAuth.post('/auth/logout/asdelegated');

                expect(response.status).toEqual(502);
                expect(String(response.headers['set-cookie'])).toMatch(/delegationId=;/);
            });
        });
    });

    describe('when backend proxy request', () => {
        it('sends X-Crm-Delegation-Id', async() => {
            let request = null;

            server.use(
                rest.get('http://crm.backend/some/proxy/handler', (req, res, ctx) => {
                    request = req;
                    return res(ctx.status(200));
                }),
            );

            await requestAuth.get('/some/proxy/handler').set('Cookie', `delegationId=1;${AUTH_COOKIE}`);
            expect(request.headers.get('x-crm-delegation-id')).toEqual('1');
        });

        describe('when delegation error', () => {
            it('removes cookie', async() => {
                server.use(
                    rest.post('http://crm.backend/some/proxy/handler', (req, res, ctx) => {
                        return res(ctx.status(403), ctx.json({ code: 'delegation-error' }));
                    }),
                );

                const response = await requestAuth.post('/some/proxy/handler');

                expect(response.status).toEqual(403);
                expect(String(response.headers['set-cookie'])).toMatch(/delegationId=;/);
            });
        });
    });
});
