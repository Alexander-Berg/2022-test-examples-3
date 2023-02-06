'use strict';
const assert = require('assert');
const request = require('supertest-as-promised');
const yasmkit = require('../');
const metrics = require('../lib/metrics/store').metrics;
const events = require('../lib/eventstore').events;

const host = 'http://localhost:8081';

describe('Server HTTP API', function() {
    this.timeout(2000);
    before(function(done) {
        Object.keys(metrics).forEach(k => delete metrics[k]);
        Object.keys(events).forEach(k => delete events[k]);
        yasmkit.server.run();
        setTimeout(done, 500);
    });
    describe('routing', function() {
        it('should return 404 for non-existent route', function() {
            return request(host)
                .get('/notfound')
                .expect(404);
        });
    });
    describe('/unistat', function() {
        beforeEach(function() {
            Object.keys(metrics).forEach(k => delete metrics[k]);
            Object.keys(events).forEach(k => delete events[k]);
        });

        it('return empty response if no metrics', function() {
            return request(host)
                .get('/unistat')
                .expect(200)
                .then(res => {
                    assert.deepEqual(res.body, []);
                });
        });
        it('return empty response if no events', function() {
            return request(host)
                .get('/unistat')
                .expect(200)
                .then(res => {
                    assert.deepEqual(res.body, []);
                });
        });
        it('return metric data', function() {
            yasmkit.metrics.addMaxLine('test_max');
            yasmkit.addEvent('test_max', 10);
            return request(host)
                .get('/unistat')
                .expect(200)
                .then(res => {
                    assert.deepEqual(res.body, [['test_max_axxx', 10]]);
                });
        });
        it('should increment summ', function() {
            yasmkit.metrics.addSummLine('test_summ_incr');
            yasmkit.addEvent('test_summ_incr', 10);
            return request(host)
                .get('/unistat')
                .expect(200)
                .then(res => {
                    assert.deepEqual(res.body, [['test_summ_incr_dmmm', 10]]);
                    yasmkit.addEvent('test_summ_incr', 20);
                    return request(host)
                        .get('/unistat')
                        .expect(200);
                }).then(res => {
                    assert.deepEqual(res.body, [['test_summ_incr_dmmm', 30]]);
                });
        });
        it('should flush metrics with abs aggregation', function() {
            yasmkit.metrics.addMaxLine('test_abs_flush');
            yasmkit.addEvent('test_abs_flush', 20);
            return request(host)
                .get('/unistat')
                .expect(200)
                .then(res => {
                    assert.deepEqual(res.body, [['test_abs_flush_axxx', 20]]);
                    yasmkit.addEvent('test_abs_flush', 10);
                    return request(host)
                        .get('/unistat')
                        .expect(200);
                }).then(res => {
                    assert.deepEqual(res.body, [['test_abs_flush_axxx', 10]]);
                });
        });
        it('should return correctly sorted histogram', function() {
            yasmkit.metrics.addHistogram('test_hgram');
            yasmkit.addEvent('test_hgram', 1);
            yasmkit.addEvent('test_hgram', 200);
            yasmkit.addEvent('test_hgram', 30);
            yasmkit.addEvent('test_hgram', 31);
            return request(host)
                .get('/unistat')
                .expect(200)
                .then(res => {
                    const hgram = res.body.find(metric => metric[0] === 'test_hgram_ahhh')[1];
                    assert.equal(hgram.length, 3);
                    for (let i = 1; i < hgram.length; i++) {
                        assert(hgram[i - 1][0] < hgram[i][0]);
                    }
                    assert.equal(hgram[1][1], 2);
                });
        });
        describe('onReadUnistat', function() {
            it('should not throw if not debug and handler is not a function', function() {
                yasmkit.server.onReadUnistat('string');
            });
            it('should support synchronous function', function() {
                yasmkit.metrics.addMaxLine('on_read_unistat_sync');
                yasmkit.server.onReadUnistat(addEvent => {
                    addEvent('on_read_unistat_sync', 100);
                });
                return request(host)
                    .get('/unistat')
                    .expect(200)
                    .then(res => {
                        const val = res.body.find(m => m[0].startsWith('on_read_unistat_sync'))[1];
                        assert.equal(val, 100);
                    });
            });
            it('should support function, returning Promise', function() {
                yasmkit.metrics.addMaxLine('on_read_unistat_async');
                yasmkit.server.onReadUnistat(addEvent => {
                    return Promise.resolve().then(() => {
                        addEvent('on_read_unistat_async', 100);
                    });
                });
                return request(host)
                    .get('/unistat')
                    .expect(200)
                    .then(res => {
                        const val = res.body.find(m => m[0].startsWith('on_read_unistat_async'))[1];
                        assert.equal(val, 100);
                    });
            });
            it('should not fail if function throws', function() {
                yasmkit.metrics.addMaxLine('on_read_unistat_sync_fail');
                yasmkit.server.onReadUnistat(() => {
                    throw new Error();
                });
                yasmkit.server.onReadUnistat(addEvent => {
                    addEvent('on_read_unistat_sync_fail', 100);
                });
                return request(host)
                    .get('/unistat')
                    .expect(200)
                    .then(res => {
                        const val = res.body.find(m => m[0].startsWith('on_read_unistat_sync_fail'))[1];
                        assert.equal(val, 100);
                    });
            });
            it('should not fail if Promise is rejected', function() {
                yasmkit.metrics.addMaxLine('on_read_unistat_async_fail');
                yasmkit.server.onReadUnistat(() => {
                    throw new Error();
                });
                yasmkit.server.onReadUnistat(addEvent => {
                    addEvent('on_read_unistat_async_fail', 100);
                });
                return request(host)
                    .get('/unistat')
                    .expect(200)
                    .then(res => {
                        const val = res.body.find(m => m[0].startsWith('on_read_unistat_async_fail'))[1];
                        assert.equal(val, 100);
                    });
            });
        });
    });
    it('/panel', function() {
        return request(host)
            .get('/panel?panelId=test')
            .expect(200)
            .then(res => {
                assert(res.text.startsWith('<!DOCTYPE'));
            });
    });
    describe('/panel/config', function() {
        before(function() {
            yasmkit.metrics.addSummLine('test_summ');
        });
        it('should return 400 if no panelId', function() {
            return request(host)
                .get('/panel/config')
                .expect(400);
        });
        it('should return 404 if no such panel', function() {
            return request(host)
                .get('/panel/config?panelId=undefined')
                .expect(404);
        });
        it('should support metric chart', function() {
            const panel = yasmkit.server.addPanel({
                id: 'test_metric',
            });
            const chart = panel.addChart({
                title: 'chart',
            });
            chart.addSignal({
                metric: 'test_summ',
            });
            return request(host)
                .get('/panel/config?panelId=test_metric')
                .expect(200)
                .then(res => {
                    assert.equal(res.body.charts[0].signals[0].name, 'unistat-test_summ_dmmm');
                });
        });
        it('should support custom functions (autohandlers)', function() {
            yasmkit.server.addPanel({
                id: 'test_func',
                charts: [{
                    title: 'chart',
                    signals: [{
                        metric: 'test_summ',
                        func: ['max', 10],
                    }],
                }],
            });
            return request(host)
                .get('/panel/config?panelId=test_func')
                .expect(200)
                .then(res => {
                    assert.equal(res.body.charts[0].signals[0].name, 'max(unistat-test_summ_dmmm,10)');
                });
        });
        it('should support custom metrics', function() {
            yasmkit.server.addPanel({
                id: 'test_custom',
                charts: [{
                    title: 'chart',
                    signals: [{
                        name: 'test_summ_dmmm',
                    }],
                }],
            });
            return request(host)
                .get('/panel/config?panelId=test_custom')
                .expect(200)
                .then(res => {
                    assert.equal(res.body.charts[0].signals[0].name, 'test_summ_dmmm');
                });
        });
    });
});
