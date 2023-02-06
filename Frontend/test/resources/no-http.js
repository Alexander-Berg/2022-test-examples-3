let vow = require('vow');
let Resource = require('./setup');
let ResourceNoHTTP = Resource.create();

ResourceNoHTTP.prototype.get = function(opts) {
    if (! opts.please) {
        return vow.reject();
    }

    return vow
        .invoke(this.processResponseMeta.bind(this, { success: true }))
        .then(this.processResultData.bind(this));
};

module.exports = ResourceNoHTTP;
