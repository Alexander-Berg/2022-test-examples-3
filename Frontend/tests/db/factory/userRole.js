const _ = require('lodash');

const BaseFactory = require('tests/db/factory/base');

class RoleFactory extends BaseFactory {
    static get defaultData() {
        return {
            id: this.id,
            login: 'noone',
            role: 'user',
            eventGroupId: null,
        };
    }

    static get table() {
        return require('db').userRole;
    }

    static unique(data) {
        return _.pick(data, ['id']);
    }

    static get subFactories() {
        return {
            eventGroupId: require('tests/db/factory/group'),
        };
    }
}

module.exports = RoleFactory;
