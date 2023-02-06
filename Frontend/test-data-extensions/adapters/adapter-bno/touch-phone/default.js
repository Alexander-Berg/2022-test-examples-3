var raiffeisen = require('./datasets/raiffeisen.dataset')(),
    app = require('./datasets/app.dataset')();

module.exports = require('./common')(app, raiffeisen, {});
