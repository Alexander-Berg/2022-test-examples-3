import { SOVETNIK_ADFOX_ACCOUT_ID } from '../../../../../../src/constants';

module.exports = {
    host: 'https://fenek.market.yandex.ru',
    route: `/${SOVETNIK_ADFOX_ACCOUT_ID}/getBulk/v1?reqid=123456&req_id=123456`,
    method: 'post',
    allowUnmocked: false,
    response: {
        jsonapi: {
            version: '1.0',
            meta: {
                protocol_version: '1.0',
            },
        },
        meta: {
            session_id: '971077377',
            request_id: '1652695245889/6399acd2ae2465e5cbbae8f4681e76f8',
        },
        data: [],
        errors: [
            {
                id: '0',
                status: '404',
            },
        ],
    },
};
