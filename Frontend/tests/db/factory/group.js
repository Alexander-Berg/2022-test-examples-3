const BaseFactory = require('tests/db/factory/base');

class GroupFactory extends BaseFactory {
    static get defaultData() {
        return {
            id: this.id,
            slug: `group_${this.id}`,
            name: `Группа_${this.id}`,
        };
    }

    static get table() {
        return require('db').groups;
    }
}

module.exports = GroupFactory;
