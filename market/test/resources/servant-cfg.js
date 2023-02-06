const Resource = require('./setup');

const ResourceMain = Resource.create();

ResourceMain.cfg = {
    servant: 'servant',
};

ResourceMain.prototype.processResponseMeta = function (...args) {
    const response = args[0];
    this._meta = response.meta;

    return this.constructor.__super.prototype.processResponseMeta.apply(this, args);
};

ResourceMain.prototype.processResultData = function (results) {
    results._meta = this._meta;

    return results;
};

module.exports = ResourceMain;
