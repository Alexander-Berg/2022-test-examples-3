const vow = require('vow');
const assign = require('object-assign');

const Multiple = require('./setup').create();

const EXTRA = {
    id: 'service',
    trustworthy: true,
};

Multiple.cfg = {
    path: '/settings',
};

Multiple
    .method('get', function () {
        return {done: true};
    })
    .method('put', {
        error() {
            return vow.resolve({ooopsie: 'error'});
        },
    })
    .method('extended', {
        prepare(opts) {
            return this.prepareRequestOpts(assign({}, opts, EXTRA));
        },
        process(data) {
            return assign(data, EXTRA);
        },
    })
    .method('action', {
        action() {
            return 'response';
        },
    });

module.exports = Multiple;
