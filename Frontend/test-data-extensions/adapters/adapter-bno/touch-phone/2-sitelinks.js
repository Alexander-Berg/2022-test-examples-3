var raiffeisen = require('./datasets/raiffeisen.dataset')(),
    app = require('./datasets/app.dataset')();

raiffeisen.sitelinks.items.splice(2);

module.exports = require('./common')(app, raiffeisen, {});
