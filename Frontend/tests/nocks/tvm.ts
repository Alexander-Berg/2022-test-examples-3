import { Dictionary } from 'lodash';
import nock from 'nock';

import config from '@yandex-int/yandex-cfg';

interface TvmOptions {
    delay?: number;
    ticket?: string;
}

interface TvmTicket {
    ticket: string | null;
}

export const nockTvm = (options: TvmOptions) => {
    const { delay = 0, ticket = null } = options;

    const response = (config.tvm.destinations as string[]).reduce((acc: Dictionary<TvmTicket | null>, dst: string) => {
        acc[dst] = { ticket };

        return acc;
    }, {});

    nock(config.tvm.serverUrl).get('/tvm/tickets').query(true).times(Infinity).delay(delay).reply(200, response);
};
