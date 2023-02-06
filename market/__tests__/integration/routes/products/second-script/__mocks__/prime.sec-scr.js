/* eslint-disable max-len */
import { REPORT_DEV_HOST, REPORT_DEV_PORT, REPORT_DEV_PATH } from '../../../../../../src/env';

const HOST = `${REPORT_DEV_HOST}:${REPORT_DEV_PORT}`;

const ROUTE = new RegExp(`/${REPORT_DEV_PATH}`);

const RESPONSE = {
    redirect: {
        params: {
            text: ['Отделочный материал "Гибкий камень"'],
            hid: ['658853'],
            rt: ['9'],
            nid: ['18060970'],
            slug: ['oblitsovochnyi-kamen'],
            was_redir: ['1'],
            srnum: ['340'],
            rs: ['eJwzYgpgBAABcwCG'],
        },
        target: 'search',
    },
};

module.exports = {
    host: HOST,
    route: ROUTE,
    response: RESPONSE,
};
