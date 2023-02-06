const Resource = require('./setup');

const ResourceWithCustomCert = Resource.create();

ResourceWithCustomCert.cfg = {
    path: '/test',
    timeout: 300,
};

ResourceWithCustomCert.prototype.processResponseMeta = function (...args) {
    const response = args[0];
    this._meta = response.meta;
    this._statusCode = response.statusCode;

    return this.constructor.__super.prototype.processResponseMeta.apply(this, args);
};

ResourceWithCustomCert.prototype.processResultData = function (results) {
    if (results === null) {
        results = {};
    }
    results._meta = this._meta;
    results._meta.statusCode = this._statusCode;

    return results;
};

module.exports = ResourceWithCustomCert;
