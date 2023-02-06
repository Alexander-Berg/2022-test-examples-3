const { create, Entity } = require('@yandex-int/bem-page-object');
const createElems = require('./elems');

class CommonEntity extends Entity {
    static preset() {
        return 'react';
    }
}
const PO = create(createElems(CommonEntity));

module.exports = PO;
