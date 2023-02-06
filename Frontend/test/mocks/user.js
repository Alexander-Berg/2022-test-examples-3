let sinon = require('sinon');
let extend = require('../../../nodules-libs').util.extend;

module.exports = function createMockUser(addition) {
    return extend(true, {
        auth: {
            checkCRC: sinon.stub().returns(true),
        },
        l10n: {
            lang: 'ru',
            locale: {
                id: 'ru',
            },
        },
    }, addition);
};
