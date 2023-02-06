const { create } = require('../../../../../../vendors/hermione');

function getPlarformElems(platform) {
    if (platform === 'touch-phone') {
        return require('./index@touch-phone');
    }

    return require('./index@desktop');
}

module.exports = platform => create(getPlarformElems(platform));
