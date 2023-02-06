import { RequestError } from '@yandex-int/apphost-lib';
import { NAppHostProtocol } from '@crm/protos';
import { createTVMCheckMiddleware } from '../createTVMCheckMiddleware';

const createApphostContext = (hasTicket = true) => {
    const request = Buffer.from(
        NAppHostProtocol.TServiceRequest.encode({
            Ticket: hasTicket ? 'ticket' : undefined,
        }).finish(),
    );

    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const ctx: any = {
        getRawRequestData: () => request,
    };

    return { ctx };
};

describe('createTVMCheckMiddleware', () => {
    it('checks tvm ticket without error', async () => {
        const { ctx } = createApphostContext();
        const middleware = createTVMCheckMiddleware({
            tvmClient: {
                // eslint-disable-next-line @typescript-eslint/no-explicit-any
                checkServiceTicket: () => Promise.resolve(true as any),
                getTickets: () => Promise.resolve({}),
            },
        });

        await middleware(ctx);
    });

    it('rejects tvm ticket with error', async () => {
        const { ctx } = createApphostContext();
        const middleware = createTVMCheckMiddleware({
            tvmClient: {
                checkServiceTicket: () => Promise.reject(new Error('error')),
                getTickets: () => Promise.resolve({}),
            },
        });

        try {
            await middleware(ctx);
        } catch (error) {
            expect(error).toBeInstanceOf(RequestError);
            expect((error as Error).message).toBe('error');
        }
    });

    it('rejects tvm ticket with no error', async () => {
        const { ctx } = createApphostContext();
        const middleware = createTVMCheckMiddleware({
            tvmClient: {
                checkServiceTicket: () => Promise.reject('error'),
                getTickets: () => Promise.resolve({}),
            },
        });

        try {
            await middleware(ctx);
        } catch (error) {
            expect(error).toBeInstanceOf(RequestError);
            expect((error as Error).message).toBe(
                'Unknown service ticket check error',
            );
        }
    });

    it('rejects tvm ticket with no ticket error', async () => {
        const { ctx } = createApphostContext(false);
        const middleware = createTVMCheckMiddleware({
            tvmClient: {
                checkServiceTicket: () => Promise.reject('error'),
                getTickets: () => Promise.resolve({}),
            },
        });

        try {
            await middleware(ctx);
        } catch (error) {
            expect(error).toBeInstanceOf(RequestError);
            expect((error as Error).message).toBe('No service ticket');
        }
    });
});
