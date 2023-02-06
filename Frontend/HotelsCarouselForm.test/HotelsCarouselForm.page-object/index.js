const { create } = require('../../../../../../vendors/hermione');
const desktop = require('./index@desktop');

module.exports = {
    desktop: create(desktop),
};
