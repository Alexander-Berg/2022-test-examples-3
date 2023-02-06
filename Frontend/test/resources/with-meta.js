let Resource = require('./setup');
let ResourceWithMeta = Resource.create();

ResourceWithMeta.cfg = {
    path: '/test',
    timeout: 300,
    maxRetries: 1,
};

ResourceWithMeta.prototype.processResponseMeta = function(response) {
    this._meta = response.meta;

    return this.constructor.__super.prototype.processResponseMeta.apply(this, arguments);
};

ResourceWithMeta.prototype.processResultData = function(results) {
    results._meta = this._meta;

    return results;
};

module.exports = ResourceWithMeta;
