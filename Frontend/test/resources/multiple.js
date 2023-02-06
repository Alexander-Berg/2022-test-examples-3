/* eslint-disable import/no-extraneous-dependencies */

let vow = require('vow');
let extend = require('extend');
let Multiple = require('./setup').create();
let EXTRA = {
    id: 'service',
    trustworthy: true,
};

Multiple.cfg = {
    path: '/settings',
};

Multiple
    .method('get', function() {
        return { done: true };
    })
    .method('put', {
        error: function() {
            return vow.resolve({ ooopsie: 'error' });
        },
    })
    .method('extended', {
        prepare: function(opts) {
            return this.prepareRequestOpts(extend(opts, EXTRA));
        },
        process: function(data) {
            return extend(data, EXTRA);
        },
    })
    .method('action', {
        action: function() {
            return 'response';
        },
    });

module.exports = Multiple;
