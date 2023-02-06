'use strict';
const assert = require('assert');
const aggregations = require('../lib/aggregations');

describe('Aggregations', function() {
    it('aver', function() {
        assert.deepEqual(aggregations.aver({ sum: 2, count: 1 }, 4), { sum: 6, count: 2 });
        assert.deepEqual(aggregations.aver({ sum: 2, count: 1 }), { sum: 2, count: 1 });
        assert.deepEqual(aggregations.aver(null, 2), { sum: 2, count: 1 });
        assert.deepEqual(aggregations.aver(undefined, 2), { sum: 2, count: 1 });
    });

    it('summ', function() {
        assert.equal(aggregations.summ(1, 2), 3);
        assert.equal(aggregations.summ(1), 2);
        assert.equal(aggregations.summ(1, 0), 1);
        assert.equal(aggregations.summ(null, 2), 2);
        assert.equal(aggregations.summ(undefined, 2), 2);
    });

    it('max', function() {
        assert.equal(aggregations.max(1, 2), 2);
        assert.equal(aggregations.max(-1, 0), 0);
        assert.equal(aggregations.max(0, -1), 0);
        assert.equal(aggregations.max(null, -2), -2);
        assert.equal(aggregations.max(undefined, 2), 2);
        assert.equal(aggregations.max(2), 2);
    });

    it('min', function() {
        assert.equal(aggregations.min(1, 2), 1);
        assert.equal(aggregations.min(-1, 0), -1);
        assert.equal(aggregations.min(0, -1), -1);
        assert.equal(aggregations.min(null, -2), -2);
        assert.equal(aggregations.min(undefined, 2), 2);
        assert.equal(aggregations.min(2), 2);
    });

    it('last', function() {
        assert.equal(aggregations.last(1, 2), 2);
        assert.equal(aggregations.last(1, 0), 0);
        assert.equal(aggregations.last(null, 2), 2);
        assert.equal(aggregations.last(undefined, 2), 2);
    });

    describe('hgram', function() {
        const HGRAM_FACTOR = 1.6;
        const hgramOptions = { bucketFactor: HGRAM_FACTOR };
        function getBucketBound(val, hgramFactor) {
            hgramFactor = hgramFactor || HGRAM_FACTOR;
            return Math.pow(hgramFactor, Math.floor(Math.log(val) / Math.log(hgramFactor)));
        }

        it('should add one item to empty histogram', function() {
            assert.equal(aggregations.hgram(null, 20, hgramOptions).get(getBucketBound(20)), 1);
        });
        it('should increment bucket value if bucket already exists', function() {
            const hgram = new Map();
            hgram.set(getBucketBound(20), 1);
            assert.equal(aggregations.hgram(hgram, 21, hgramOptions).get(getBucketBound(21)), 2);
        });
        it('should add new bucket if needed', function() {
            const hgram = new Map();
            hgram.set(getBucketBound(20), 1);
            assert.equal(aggregations.hgram(hgram, 210, hgramOptions).get(getBucketBound(210)), 1);
        });
        it('should set bucket factor to 1.2 by default', function() {
            assert.equal(aggregations.hgram(null, 20, {}).get(getBucketBound(20, 1.2)), 1);
        });
    });
});
