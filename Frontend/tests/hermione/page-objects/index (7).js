const pageObject = require('@yandex-int/bem-page-object');

const El = require('./Entity');

const elems = {};

elems.Table = new El({ block: 'Table' });

module.exports = {
    loadPageObject() {
        return pageObject.create(elems);
    },
};
