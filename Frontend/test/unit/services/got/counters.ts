/* eslint-disable */
import test from 'ava';
import { GotCounters } from '../../../../services/got/counters';

test('Should create valid counters', t => {
    const counters = new GotCounters();

    counters.createCounters('test');

    t.deepEqual(counters.counters, {
        http_source_test_requests_total_summ: 0,
        http_source_test_requests_2xx_summ: 0,
        http_source_test_requests_3xx_summ: 0,
        http_source_test_requests_4xx_summ: 0,
        http_source_test_requests_5xx_summ: 0,
        http_source_test_requests_error_summ: 0,
        http_source_test_requests_connection_error_summ: 0,
        http_source_test_requests_timeout_error_summ: 0,
        http_source_test_requests_other_error_summ: 0,
    });
});

test('Should inc counters', t => {
    const counters = new GotCounters();

    counters.createCounters('test');

    counters.incCounter('test', '2xx_summ');
    counters.incCounter('test', '3xx_summ');
    counters.incCounter('test', '4xx_summ');
    counters.incCounter('test', '5xx_summ');

    t.deepEqual(counters.counters, {
        http_source_test_requests_total_summ: 0,
        http_source_test_requests_2xx_summ: 1,
        http_source_test_requests_3xx_summ: 1,
        http_source_test_requests_4xx_summ: 1,
        http_source_test_requests_5xx_summ: 1,
        http_source_test_requests_error_summ: 0,
        http_source_test_requests_connection_error_summ: 0,
        http_source_test_requests_timeout_error_summ: 0,
        http_source_test_requests_other_error_summ: 0,
    });
});
