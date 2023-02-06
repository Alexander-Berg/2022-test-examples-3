const vow = require('vow');

const Resource = require('./setup');

const ResourceNoHTTP = Resource.create();

const method = function (opts) {
    if (!opts.please) {
        return vow.reject();
    }

    const params = {success: true};

    if (opts.meta) {
        params.meta = opts.meta;
        params.data = {};
    }

    return vow
        .invoke(this.processResponseMeta.bind(this, params))
        .then(this.processResultData.bind(this));
};

ResourceNoHTTP
    .method('get', method)
    .method('advanced', method);

module.exports = ResourceNoHTTP;
