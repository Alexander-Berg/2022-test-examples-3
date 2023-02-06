const POLL_TIMEOUT = 5000;
const RETRY_INTERVAL = 300;

/**
 * Получить из blockstat-лога дерево баобаба
 *
 * @param {Function} validator - валидатор
 * @param {String} [reqid] - reqid
 *
 * @returns {Promise<Object>}
 */
module.exports = async function yaGetCounters(validator, reqid) {
    const curReqid = reqid || await this.yaGetReqId();
    let lastCounters;
    await this.assertCounters(
        curReqid,
        {
            retryDelay: RETRY_INTERVAL,
            timeout: POLL_TIMEOUT
        },
        counters => {
            lastCounters = counters;

            return validator(counters);
        }
    );

    return lastCounters;
};
