const { get, set } = require('lodash');
const { findLastItemFromRendererSource } = require('../utils');

module.exports = function testingFlags(req, res, next) {
    const expFlags = {
        error_counter: 1,
        'analytics-disabled': 1,
        'adv-disabled': 1,
        'fallback-disabled': 1,
        'turbopages-org-override-disabled': 1,
    };
    const kotik = req.kotik;
    const { data } = findLastItemFromRendererSource(kotik.ctx, 'template_data') || {};

    if (!data) {
        return next();
    }

    const flags = get(data, 'reqdata.flags', {});

    set(data, 'reqdata.flags', Object.assign(expFlags, flags));

    next();
};
