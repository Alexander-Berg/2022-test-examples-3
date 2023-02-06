'use strict';

const MARKET_BUCKETS_HEADER = 'x-market-buckets';
const BUCKETS_COOKIE = 'sec-buckets';

const _ = require('lodash');

/**
 * Получает ID-шники экспериментов, в которые попал пользователь. Получаются из UaaS.
 *
 * Достаём бакеты из различных заголовков, в зависимости от нахождения в дефолте или в эксп пакете
 * @param {Request} request
 */
function getTestBuckets(request) {
    const user = request.getData('user');
    const isYandex = _.get(user, 'region.isInternalNetwork', false);

    let fromCookie;

    if (isYandex) {
        fromCookie = request.getCookie(BUCKETS_COOKIE);
    }

    return fromCookie || (request.splitname
        ? request.headers[MARKET_BUCKETS_HEADER]
        : _.get(request, 'abt.expBuckets', undefined));
}

module.exports = {
    getTestBuckets,
};
