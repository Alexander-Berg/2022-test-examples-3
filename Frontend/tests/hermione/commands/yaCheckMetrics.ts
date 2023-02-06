import type { IMetrics } from './yaGetMetrics';

const INITIAL_DELAY = 100; // Время перед первой проверкой метрики
const RETRY_INTERVAL = 300; // Время ожидания между повторными проверками метрик
const POLL_TIMEOUT = 30 * 1000; // Максимальное время, после которого проверка метрик будет считаться неудачной

/**
 * Проверяет соответствие рассчитанных метрик ожидаемым значениям.
 * @see https://a.yandex-team.ru/arc_vcs/frontend/projects/infratest/packages/hermione-get-counters#checkmetricsexpectedmetrics-initialdelay-retrydelay-timeout-2
 */
export async function yaCheckMetrics(
    this: WebdriverIO.Browser,
    expectedMetrics: Partial<IMetrics>,
) {
    if (Object.keys(expectedMetrics).length === 0) return;

    return this.checkMetrics(expectedMetrics, {
        initialDelay: INITIAL_DELAY,
        retryDelay: RETRY_INTERVAL,
        timeout: POLL_TIMEOUT,
    });
}
