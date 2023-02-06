import { Core as DuffmanCore } from '@duffman-int/core';
import directory, { id } from '../service';

class Core extends DuffmanCore<Record<string, never>, { [id]: typeof directory }> {
    static services = {
        [id]: directory,
    }

    static config = {
        services: {
            [id]: {
                url: 'http://@',
            },
        },
    }
}

function prepare() {
    const req: any = {
        requestId: 'reqid',
        ip: '::1',
        headers: {
            'x-real-ip': '::1',
        },
    };
    const res: any = {
        on: jest.fn(),
    };

    res.req = req;
    req.res = res;

    const core = new Core(req, res);
    const got = jest.spyOn(core, 'got').mockImplementation(async() => void 0);

    core.setTvmTickets({
        directory: '3:serv:FAKE',
    });
    core.auth.set({
        uid: '100500',
        login: 'fake',
        userTicket: '3:user:FAKE',
    } as any);

    return { core, got };
}

describe('duffman core integration', () => {
    it('default', async() => {
        const { core, got } = prepare();

        await core.service(id)('/test', { a: 1 });

        expect(got).toHaveBeenCalledWith(
            'http://@/test',
            {
                headers: {
                    'Yandex-Cloud-Request-ID': 'reqid',
                    'x-uid': '100500',
                    'x-user-ip': '::1',
                    'x-ya-service-ticket': '3:serv:FAKE',
                    'x-ya-user-ticket': '3:user:FAKE',
                },
                json: true,
                timeout: 3000,
                retryOnTimeout: 0,
                retryPolicy: {
                    statsPeriod: 5,
                    budget: 0.2,
                },
                query: { a: 1 },
            },
        );
    });
});
