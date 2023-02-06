import type { ICounter } from '../../typings';

const POLL_TIMEOUT = 5000;
const RETRY_INTERVAL = 300;

/** Получить из blockstat-лога дерево баобаба */
export async function yaGetCounters(
    this: WebdriverIO.Browser,
    validator: (counter: ICounter) => ICounter | void,
    reqid?: string,
) {
    const curReqid = reqid || await this.yaGetReqId();
    let lastCounters: ICounter | undefined;

    await this.assertCounters(
        curReqid,
        {
            retryDelay: RETRY_INTERVAL,
            timeout: POLL_TIMEOUT,
        },
        counters => {
            lastCounters = counters;
            return validator(counters);
        },
    );

    return lastCounters;
}
