const { create } = require('../../../../vendors/hermione');

const PO = {
    desktop: require('./index@desktop'),
    'touch-phone': require('./index@touch-phone'),
};

module.exports = function(platform) {
    return create(PO[platform]);
};
