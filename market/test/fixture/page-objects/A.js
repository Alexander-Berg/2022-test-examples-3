const {PageObject} = require('../../../');

class A extends PageObject {
    static get root() {
        return '.a';
    }
}

module.exports = A;
