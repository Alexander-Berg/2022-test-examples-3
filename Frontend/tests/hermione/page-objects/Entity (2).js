const { Entity } = require('@yandex-int/bem-page-object');

class ReactEntity extends Entity {
    static preset() {
        return 'react'; // нейминг Block-Elem_mod
    }
}

module.exports = {
    ReactEntity,
};
