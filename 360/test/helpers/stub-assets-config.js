'use strict';

const mockRequire = require('mock-require');

mockRequire('../../assets-config.js', {
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
});
