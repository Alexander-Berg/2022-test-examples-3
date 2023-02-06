const { create } = require('../../../../../../vendors/hermione');
const touchPhone = require('./index@touch-phone');
const desktop = require('./index@desktop');

module.exports = {
    desktop: create(desktop),
    touchPhone: create(touchPhone),
};
