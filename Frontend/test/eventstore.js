'use strict';
const assert = require('assert');
const urequire = require('require-uncached');
const eventStore = urequire('../lib/eventstore');
const yasmkit = urequire('../');

describe('eventstore', function() {
    before(function() {
        yasmkit.metrics.addMaxLine('abs_aggr');
        yasmkit.metrics.addSummLine('delta_aggr');
    });

    beforeEach(function() {
        Object.keys(eventStore.events).forEach(k => delete eventStore.events[k]);
    });

    it('flush()', function() {
        it('should clean metrics with resetOnRead: true', function() {
            eventStore.addEvent('abs_aggr', 10);
            eventStore.addEvent('abs_aggr', 20);
            assert(eventStore.events.abs_aggr);
            const val = eventStore.events.delta_aggr;
            eventStore.flush();
            assert(!eventStore.events.abs_aggr);
            assert.strictEqual(val, eventStore.events.delta_aggr);
        });
    });

    it('addEvent()', function() {
        eventStore.addEvent('abs_aggr', 10);
        eventStore.addEvent('delta_aggr', 10);
        assert(eventStore.events.abs_aggr);
        assert(eventStore.events.delta_aggr);
    });
});
