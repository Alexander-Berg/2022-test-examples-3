/* eslint-disable max-len */
import { REPORT_DEV_HOST, REPORT_DEV_PORT, REPORT_DEV_PATH } from '../../../../../../src/env';

const HOST = `${REPORT_DEV_HOST}:${REPORT_DEV_PORT}`;

const ROUTE = new RegExp(`/${REPORT_DEV_PATH}`);

const RESPONSE = {
    redirect: {
        params: {
            text: ['2 ТБ Жесткий диск Seagate BarraCuda [ST2000DM008]'],
            hid: ['91033'],
            modelid: ['130084187'],
            rt: ['4'],
            nid: ['55316'],
            slug: ['zhestkii-disk-seagate-barracuda-2-tb-st2000dm008'],
            was_redir: ['1'],
            srnum: ['418'],
            rs: ['eJwzYgpgBAABcwCG'],
        },
        target: 'product',
    },
};

module.exports = {
    host: HOST,
    route: ROUTE,
    response: RESPONSE,
};
