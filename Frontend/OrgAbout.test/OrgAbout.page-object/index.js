const { create } = require('../../../../../../vendors/hermione');
const touchPhone = require('./index@touch-phone');
const desktop = require('./index@desktop');

module.exports = {
    touchPhone: create(touchPhone),
    desktop: create(desktop),
};
