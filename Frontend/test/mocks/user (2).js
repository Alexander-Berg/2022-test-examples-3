var extend = require('nodules-libs').util.extend,
    sinon = require('sinon');

module.exports = function createMockUser(addition) {
    return extend(true, {
        auth: {
            checkCRC: sinon.stub().returns(true)
        },
        l10n: {
            lang: 'ru',
            locale: {
                id: 'ru'
            }
        }
    }, addition);
};
