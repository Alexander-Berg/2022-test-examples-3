'use strict';
const assert = require('assert');
const urequire = require('require-uncached');
const metricStore = require('../lib/metrics/store'); // Так нельзя делать, но иначе, кажется, никак
const yasmkit = urequire('../');

describe('metrics api', function() {
    // FIXME: Переосмыслить эти тесты, они очень плохи и не работают
    describe.skip('add ...', function() {
        it('addHistogram()', function() {
            yasmkit.metrics.addHistogram('sample_hgram');
            const metric = metricStore.metrics.sample_hgram;
            assert.equal(metric.type, 'hgram');
            const aggr = metric.aggregation;
            assert.equal(aggr.group, 'hgram');
            assert.equal(aggr.history, 'hgram');
            assert.equal(aggr.instance, 'hgram');
        });

        it('addSummLine()', function() {
            yasmkit.metrics.addSummLine('sample_summ');
            const metric = metricStore.metrics.sample_summ;
            assert.equal(metric.type, 'line');
            const aggr = metric.aggregation;
            assert.equal(aggr.group, 'summ');
            assert.equal(aggr.history, 'summ');
            assert.equal(aggr.instance, 'summ');
        });

        it('addMaxLine()', function() {
            yasmkit.metrics.addMaxLine('sample_max');
            const metric = metricStore.metrics.sample_max;
            assert.equal(metric.type, 'line');
            const aggr = metric.aggregation;
            assert.equal(aggr.group, 'max');
            assert.equal(aggr.history, 'max');
            assert.equal(aggr.instance, 'max');
        });

        it('addMinLine()', function() {
            yasmkit.metrics.addMinLine('sample_min');
            const metric = metricStore.metrics.sample_min;
            assert.equal(metric.type, 'line');
            const aggr = metric.aggregation;
            assert.equal(aggr.group, 'min');
            assert.equal(aggr.history, 'min');
            assert.equal(aggr.instance, 'min');
        });

        it('addCustomLine()', function() {
            yasmkit.metrics.addCustomLine('sample_custom', {
                type: 'line',
                aggregation: {
                    instance: 'summ',
                    agent: 'delta',
                    group: 'last',
                    history: 'max',
                },
            });
            const metric = metricStore.metrics.sample_custom;
            assert.equal(metric.type, 'line');
            const aggr = metric.aggregation;
            assert.equal(aggr.group, 'last');
            assert.equal(aggr.history, 'max');
            assert.equal(aggr.instance, 'summ');
        });
    });

    it('has()', function() {
        yasmkit.metrics.addMaxLine('has_max');
        assert.strictEqual(yasmkit.metrics.has('has_max'), true);
        assert.strictEqual(yasmkit.metrics.has('nonexistent'), false);
    });
});
