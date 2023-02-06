const { create } = require('../../../../../../vendors/hermione');
const desktop = require('./index@desktop');
const touchPhone = require('./index@touch-phone');

module.exports = {
    desktop: create(desktop),
    touchPhone: create(touchPhone),
};
