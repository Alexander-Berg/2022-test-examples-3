'use strict';
const assert = require('assert');
const urequire = require('require-uncached');
const metricStore = urequire('../lib/metrics/store');
const config = require('../lib/config').getSetConfig;

describe('metric store', function() {
    afterEach(function() {
        Object.keys(metricStore.metrics).forEach(key => delete metricStore.metrics[key]);
    });

    it('should add metric', function() {
        metricStore.addMetric('sample', {
            type: 'hgram',
            aggregation: {
                instance: 'hgram',
                agent: 'absolute',
                group: 'hgram',
                history: 'hgram',
            },
        });
        assert(metricStore.metrics.sample);
    });
    it('addMetric() should set correct sigopt suffix', function() {
        metricStore.addMetric('sample', {
            type: 'line',
            aggregation: {
                instance: 'summ',
                agent: 'delta',
                group: 'lvalue',
                history: 'summ',
            },
        });
        assert.equal(metricStore.metrics.sample.sigoptSuffix, '_dttm');
    });
    it('addMetric() should validate aggregations', function() {
        config('debug', true);
        assert.throws(function() {
            metricStore.addMetric('sample', {
                type: 'line',
                aggregation: {
                    instance: 'lvalue',
                    agent: 'summ',
                    group: 'lvalue',
                    history: 'summ',
                },
            });
        });
        assert.throws(function() {
            metricStore.addMetric('sample', {
                type: 'line',
                aggregation: {
                    instance: 'lvalue',
                    agent: 'summ',
                    group: 'lvalue',
                    history: 'last',
                },
            });
        });
        config('debug', false);
    });
    it('trnsp|lvalue is aliases for last aggregation', function() {
        metricStore.addMetric('sample', {
            type: 'line',
            aggregation: {
                instance: 'lvalue',
                agent: 'delta',
                group: 'trnsp',
                history: 'summ',
            },
        });
        assert.equal(metricStore.metrics.sample.aggregation.instance, 'last');
        assert.equal(metricStore.metrics.sample.aggregation.group, 'last');
    });

    it('hasMetric()', function() {
        metricStore.addMetric('sample', {
            type: 'line',
            aggregation: {
                instance: 'summ',
                agent: 'delta',
                group: 'summ',
                history: 'summ',
            },
        });
        assert.strictEqual(metricStore.hasMetric('sample'), true);
        assert.strictEqual(metricStore.hasMetric('nonexistent'), false);
    });
});
