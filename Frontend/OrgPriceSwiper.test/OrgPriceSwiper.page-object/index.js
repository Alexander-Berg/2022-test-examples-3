const { create } = require('../../../../../../vendors/hermione');
const touchPhone = require('./index@touch-phone');

module.exports = {
    'touch-phone': create(touchPhone),
};
