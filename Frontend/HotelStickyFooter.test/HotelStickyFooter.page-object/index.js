const { create } = require('../../../../../../vendors/hermione');

module.exports = platform => create(require(`./index@${platform}`));
