const { create } = require('../../../../../../vendors/hermione');
const touchPhone = require('./index@touch-phone');

module.exports = {
    touchPhone: create(touchPhone),
};
