'use strict';
global.require = require('require-uncached');

describe('YasmKit', function() {
    describe('Internal components', function() {
        require('./aggregations');
        require('./eventstore');
        require('./metricstore');
        require('./panelstore');
        require('./debug');
    });
    describe('JS API', function() {
        require('./metricsapi');
        require('./config');
        require('./serverapi');
    });
    describe('HTTP API', function() {
        require('./server');
    });
});
