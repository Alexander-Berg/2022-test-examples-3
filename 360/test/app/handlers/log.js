'use strict';

const console_ = require('../../../helpers/console.js');

module.exports = (req, res, next) => {
    console_.log('REQUEST', req.url);
    next();
};
