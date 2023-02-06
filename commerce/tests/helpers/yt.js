'use strict';

const _ = require('lodash');
const config = require('yandex-config').yt;
const nock = require('nock');

// eslint-disable-next-line complexity
module.exports = function nockYT(options) {
    const {
        create,
        proxy,
        write,
        list,
        read,
        remove,
        query
    } = options;

    const heavyProxyName = _.get(proxy, 'response.0', '');

    const lightNock = nock(config.host);

    if (proxy) {
        lightNock
            .get(/hosts/)
            .times(proxy.times || 1)
            .reply(proxy.code || 200, proxy.response);
    }

    if (list) {
        lightNock
            .get(/list/)
            .query(query || true)
            .times(list.times || 1)
            .reply(list.code || 200, list.response);
    }

    if (create) {
        lightNock
            .post(/create/)
            .times(create.times || 1)
            .reply(create.code || 200, create.response);
    }

    if (remove) {
        lightNock
            .post(/remove/)
            .times(remove.times || 1)
            .reply(remove.code || 200, remove.response);
    }

    const heavyNock = nock(`http://${heavyProxyName}${config.api}`);

    if (write) {
        heavyNock
            .put(/write_table/)
            .times(write.times || 1)
            .reply(write.code || 200, write.response);
    }

    if (read) {
        heavyNock
            .get(/read_table/)
            .times(read.times || 1)
            .reply(read.code || 200, read.response);
    }

    return { lightNock, heavyNock };
};
