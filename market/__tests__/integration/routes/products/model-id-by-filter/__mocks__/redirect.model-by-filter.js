/* eslint-disable max-len */
import { REPORT_DEV_HOST, REPORT_DEV_PORT, REPORT_DEV_PATH } from '../../../../../../src/env';

const HOST = `${REPORT_DEV_HOST}:${REPORT_DEV_PORT}`;

const ROUTE = new RegExp(`/${REPORT_DEV_PATH}`);

const RESPONSE = {
    redirect: {
        params: {
            text: ['Электрическая зубная щетка Braun Oral-B Vitality D12.514K Frozen'],
            hid: ['278374'],
            glfilter: ['7893318:7687132'],
            rt: ['11'],
            nid: ['18049526'],
            slug: ['elektricheskie-zubnye-shchetki'],
            was_redir: ['1'],
            srnum: ['115'],
            rs: [
                'eJwzOsOodJyRK-LC2gu7L2y9sOti08WGCzsutl_YerHxwq4LGy72K1zYfrH5wsYLe8Gci51AmSaQjIJTUWJpnoJ_UWKOrpNCWGZJYk5mSaWCi6GRnqmhibeCW1F-VWqewLFHD5mVWDgYBBiAJJ-AggZDFs0sC2AEAMNQZUE,',
            ],
        },
        target: 'search',
    },
};

module.exports = {
    host: HOST,
    route: ROUTE,
    response: RESPONSE,
};
