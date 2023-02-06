const { create } = require('../../../../../../vendors/hermione');
const touch = require('./index@touch-phone');

module.exports = {
    touch: create(touch),
};
