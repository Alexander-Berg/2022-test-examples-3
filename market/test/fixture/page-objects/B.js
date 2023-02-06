const {PageObject} = require('../../../');

class B extends PageObject {
    static get root() {
        return '.b';
    }
}

module.exports = B;
