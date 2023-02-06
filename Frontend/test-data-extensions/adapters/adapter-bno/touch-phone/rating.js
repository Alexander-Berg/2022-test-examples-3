var raiffeisen = require('./datasets/raiffeisen.dataset')(),
    meta = require('./datasets/meta.dataset')();

module.exports = require('./common')({}, raiffeisen, meta);
