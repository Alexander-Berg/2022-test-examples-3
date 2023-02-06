var raiffeisen = require('./datasets/raiffeisen.dataset')(),
    app = require('./datasets/app.dataset')();

raiffeisen.sitelinks = undefined;

module.exports = require('./common')(app, raiffeisen, {});
