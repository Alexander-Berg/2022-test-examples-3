const { create, Entity } = require('@yandex-int/bem-page-object');
const createElems = require('./elems');

const PO = createElems(Entity);

module.exports = create(PO);
