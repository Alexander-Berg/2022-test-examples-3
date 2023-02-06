'use strict';

const path = require('path');

module.exports = {
    assets: {
        paths: {
            'test-module.js': '/test/compiled/url/file.js',
            'test-module.css': '/test/compiled/url/file.css'
        },
        entries: {
            'test-module.js': 1425,
            'test-module.css': 14234
        }
    }
};

require.cache[path.resolve(__dirname, '../../assets-config-watcher.js')] = module;
