const {PageObject} = require('../../../../');

class C extends PageObject {
    static get root() {
        return '.c';
    }
}

module.exports = C;
