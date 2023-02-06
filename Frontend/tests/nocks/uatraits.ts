import nock from 'nock';

import config from '@yandex-int/yandex-cfg';

interface UatraitsOptions {
    response?: object;
}

export const nockUatraits = (options: UatraitsOptions) => {
    const { response = {} } = options;

    if (!config.httpUatraits?.server) {
        throw new Error('config.httpUatraits.server not specified');
    }

    nock(config.httpUatraits.server).get('/v0/detect').query(true).times(Infinity).reply(200, response);
};
