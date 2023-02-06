import { rest, RestRequest, PathParams } from 'msw';
import { setupServer } from 'msw/node';
import { RequestError } from 'got';
import { TVMApiClientOptions } from 'typings/TVMApiClient';
import { TVMApiClient } from './TVMApiClient';
import { TVMApiCheckServiceTicketError } from './TVMApiCheckServiceTicketError';

const requestCall = jest.fn((_req: RestRequest<never, PathParams>) => {});

const server = setupServer(
    rest.get('http://local.tvm/ping', (req, res, ctx) => {
        return res(ctx.status(200), ctx.json({ status: 'OK' }));
    }),

    rest.get('http://local.tvm/v2/tickets', (req, res, ctx) => {
        const dsts = req.url.searchParams.getAll('dsts');

        requestCall(req);

        return res(
            ctx.status(200),
            ctx.json(
                dsts.reduce((ac, dst, index) => {
                    ac[dst] = {
                        ticket: 'ticket',
                        tvm_id: index,
                    };

                    return ac;
                }, {} as Record<string, unknown>),
            ),
        );
    }),

    rest.get('http://local.tvm/v2/check', (req, res, ctx) => {
        requestCall(req);

        return res(
            ctx.status(200),
            ctx.json({
                status: 'OK',
                service: {
                    status: 'OK',
                    src: 242,
                    debug_string: 'debug_string',
                    logging_string: 'logging_string',
                    roles: null,
                },
                user: null,
            }),
        );
    }),
);

beforeAll(() => server.listen({ onUnhandledRequest: 'bypass' }));
beforeEach(() => {
    requestCall.mockClear();
});
afterEach(() => {});
afterAll(() => server.close());

describe('TVMApiClient', () => {
    const TVM_SELF = '1';
    const TOKEN = 'token';

    const createClient = (options?: Partial<TVMApiClientOptions>) => {
        return new TVMApiClient({
            self: TVM_SELF,
            serverUrl: 'http://local.tvm',
            token: TOKEN,
            ...options,
        });
    };

    const client = createClient();

    describe('.ping', () => {
        it('returns success', async () => {
            const request = await client.ping();

            expect(request.statusCode).toBe(200);
            expect(request.body).toEqual({ status: 'OK' });
        });

        it('returns warning', async () => {
            const warningResponse = {
                status: 'WARNING',
                error: 'smth bad happend',
            };

            server.use(
                rest.get('http://local.tvm/ping', (req, res, ctx) => {
                    return res(ctx.status(206), ctx.json(warningResponse));
                }),
            );

            const request = await client.ping();

            expect(request.statusCode).toBe(206);
            expect(request.body).toEqual(warningResponse);
        });

        it('returns error', async () => {
            const errorResponse = {
                status: 'ERROR',
                error: 'smth bad happend',
            };

            server.use(
                rest.get('http://local.tvm/ping', (req, res, ctx) => {
                    return res(ctx.status(500), ctx.json(errorResponse));
                }),
            );

            try {
                await client.ping();
            } catch (error) {
                const gotError = error as RequestError;

                expect(gotError.response?.statusCode).toBe(500);
                expect(gotError.response?.body).toEqual(errorResponse);
            }
        });
    });

    describe('.checkServiceTicket', () => {
        it('sends Authorization header', async () => {
            const _request = await client.checkServiceTicket('ticket');
            expect(
                requestCall.mock.calls[0][0].headers.get('Authorization'),
            ).toEqual(TOKEN);
        });

        it('sends self param', async () => {
            const _request = await client.checkServiceTicket('ticket');
            expect(
                requestCall.mock.calls[0][0].url.searchParams.get('self'),
            ).toEqual(TVM_SELF);
        });

        it('returns success response', async () => {
            const checkResult = await client.checkServiceTicket('ticket');
            expect(checkResult.status).toEqual('OK');
        });

        it('returns error from tvm', async () => {
            server.use(
                rest.get('http://local.tvm/v2/check', (req, res, ctx) => {
                    return res(
                        ctx.status(403),
                        ctx.json({
                            status: 'ERROR',
                            service: {
                                status: 'ERROR',
                                src: 242,
                                debug_string: 'debug_string',
                                logging_string: 'logging_string',
                                roles: null,
                            },
                            user: null,
                        }),
                    );
                }),
            );

            try {
                const _checkResult = await client.checkServiceTicket('ticket');
            } catch (error) {
                expect(error).toBeInstanceOf(TVMApiCheckServiceTicketError);
            }
        });

        it('returns error by allowedTVMServiceIds', async () => {
            const client = createClient({ allowedTVMServiceIds: [1] });
            try {
                const _checkResult = await client.checkServiceTicket('ticket');
            } catch (error) {
                expect(error).toBeInstanceOf(TVMApiCheckServiceTicketError);
            }
        });
    });

    describe('.getTickets', () => {
        it('sends params', async () => {
            const _request = await client.getTickets({
                dsts: ['dst1', 'dst2'],
            });

            expect(
                requestCall.mock.calls[0][0].url.searchParams.get('self'),
            ).toEqual(TVM_SELF);
            expect(
                requestCall.mock.calls[0][0].url.searchParams.get('dsts'),
            ).toEqual(String(['dst1', 'dst2']));
        });
    });
});
