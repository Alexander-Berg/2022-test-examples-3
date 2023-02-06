var raiffeisen = require('./datasets/raiffeisen-organic-sitelinks.dataset')(),
    app = require('./datasets/app.dataset')();

module.exports = require('./common')(app, raiffeisen, {});
