const { create } = require('../../../../../../vendors/hermione');

module.exports = platfrom => create(require(`./index@${platfrom}`));
