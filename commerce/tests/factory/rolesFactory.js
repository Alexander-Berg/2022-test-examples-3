const _ = require('lodash');
const db = require('db/postgres');

function getRoleData(data) {
    return _.assign({}, {
        id: 0,
        code: 'user',
        title: 'User',
        active: 1
    }, data);
}

function *create(data) {
    const roleData = getRoleData(data);
    const res = yield db.Role.findOrCreate({
        where: { id: roleData.id },
        defaults: roleData
    });

    return res[0];
}

module.exports.create = create;
