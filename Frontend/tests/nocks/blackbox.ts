import nock from 'nock';

import config from '@yandex-int/yandex-cfg';

interface BlackboxOptions {
    params?: object;
    response?: object;
    ticket?: string;
}

export const nockBlackbox = (options: BlackboxOptions) => {
    const { params = {}, response = {}, ticket = '' } = options;

    const requestParams = {
        format: 'json',
        method: 'Session_id',
        attributes: '1008,14,34,31,32,33',
        regname: 'yes',
        getServiceTicket: '',
        phones: '',
        userip: '::ffff:127.0.0.1',
        host: '127.0.0.1',
        get_user_ticket: 'yes',
        ...params,
    };

    nock(`http://${config.blackbox.api}`)
        .get('/blackbox')
        .query(requestParams)
        .matchHeader('x-ya-service-ticket', ticket)
        .times(Infinity)
        .reply(200, response);
};
