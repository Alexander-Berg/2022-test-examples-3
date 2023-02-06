import nock from 'nock';

import config from '@yandex-int/yandex-cfg';

interface WebauthOptions {
    code?: number;
    sessionId?: string;
    sessionId2?: string;
}

export const nockWebauth = (options: WebauthOptions) => {
    const { code = 200, sessionId, sessionId2 } = options;

    nock(`https://${config.webauthApi.hostname}`)
        .get(`/${config.webauthApi.authPath}`)
        .query({
            idm_role: config.webauthApi.idmRole,
            required: 'cookies',
        })
        .matchHeader('cookie', `Session_id=${sessionId}; sessionid2=${sessionId2}`)
        .times(Infinity)
        .reply(code);
};
