import nock from 'nock';

import config from '@yandex-int/yandex-cfg';

interface UatraitsOptions {
    response?: object;
}

export const nockUatraits = (options: UatraitsOptions) => {
    const { response = {} } = options;

    nock(config.httpUatraits!.server!.toString()).get('/v0/detect').query(true).times(Infinity).reply(200, response);
};
