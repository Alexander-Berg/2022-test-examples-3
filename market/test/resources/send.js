const Resource = require('./setup');

const ResourceSend = Resource.create();

ResourceSend.cfg = {
    path: '/send',
    dataType: 'text',
};

// Backwards compatibility tests (now all Asker options can be configured w/o such overrides)
ResourceSend.prototype.multipart = function (opts) {
    let results = null;
    const reqOpts = this.prepareRequestOpts(opts);

    reqOpts.method = 'put';
    reqOpts.body = opts.body;
    reqOpts.bodyEncoding = 'multipart';

    results = this
        .makeRequest(reqOpts)
        .then(this.processResultData.bind(this));

    return results;
};

ResourceSend.prototype.string = function (opts) {
    let results = null;
    const reqOpts = this.prepareRequestOpts(opts);

    reqOpts.method = 'put';
    reqOpts.body = opts.body;
    reqOpts.bodyEncoding = 'string';

    results = this
        .makeRequest(reqOpts)
        .then(this.processResultData.bind(this));

    return results;
};

ResourceSend.prototype.buffer = function (opts) {
    let results = null;
    const reqOpts = this.prepareRequestOpts(opts);

    reqOpts.method = 'put';
    reqOpts.body = opts.body;
    reqOpts.bodyEncoding = 'raw';

    results = this
        .makeRequest(reqOpts)
        .then(this.processResultData.bind(this));

    return results;
};

module.exports = ResourceSend;
