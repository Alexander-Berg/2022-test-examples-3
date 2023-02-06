var raiffeisen = require('./datasets/raiffeisen.dataset')();

module.exports = require('./common')({}, raiffeisen, {});
