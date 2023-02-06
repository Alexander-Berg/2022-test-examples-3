const { create } = require('../../../../vendors/hermione');

module.exports = function(platform) {
    return create(require(`./index@${platform}`));
};
