const vow = require('vow');

const Resource = require('./setup');

const Cache = function () {
    this.get = function (key) {
        const ts = Date.now();

        if (key.indexOf('problem') === -1) {
            return vow.resolve({
                data: {
                    key,
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

    this.set = function () {};
};
const ResourceMain = Resource
    .create()
    .setCache(new Cache());

ResourceMain.cfg = {
    path: '/test',
    cache: {
        get: {keyTTL: 1000 * 60 * 60 * 24},
    },
};

module.exports = ResourceMain;
