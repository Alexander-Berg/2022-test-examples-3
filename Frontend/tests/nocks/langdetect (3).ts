import nock from 'nock';

import config from '@yandex-int/yandex-cfg';

interface LangdetectOptions {
    find?: object;
    list?: object;
}

export const nockLangdetect = (options: LangdetectOptions) => {
    const { find = {}, list = {} } = options;

    nock(config.httpLangdetect!.server!.toString())
        .get('/v0/find')
        .query(true)
        .times(Infinity)
        .reply(200, find)

        .get('/v0/list')
        .query(true)
        .times(Infinity)
        .reply(200, list);
};
