const Vow = require('vow');

const Resource = require('./setup');

const R = Resource.create().setCache({
    get() { return Vow.resolve(1); },
    set() {},
});

R.cfg = {
    path: '/test',
    cache: {get: true},
};

// eslint-disable-next-line consistent-return
R.prototype.getCache = function (opts) {
    if (opts.cache) {
        return {
            get() { return Vow.resolve(2); },
            set() {},
        };
    }
};

module.exports = R;
