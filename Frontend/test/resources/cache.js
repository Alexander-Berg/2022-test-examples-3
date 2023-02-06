let vow = require('vow');
let Resource = require('./setup');
let Cache = function() {
    this.get = function(key) {
        let ts = Date.now();

        if (key.indexOf('problem') === -1) {
            return vow.resolve({
                data: {
                    key: key,
                },
                meta: {
                    time: {
                        total: Date.now() - ts,
                    },
                    cache: true,
                },
            });
        }
        return vow.reject();
    };

    this.set = function() {};
};
let ResourceMain = Resource
    .create()
    .setCache(new Cache());

ResourceMain.cfg = {
    path: '/test',
    cache: {
        get: { keyTTL: 1000 * 60 * 60 * 24 },
    },
};

module.exports = ResourceMain;
