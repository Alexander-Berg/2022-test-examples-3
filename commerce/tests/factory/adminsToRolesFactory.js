const _ = require('lodash');
const db = require('db/postgres');

const adminsFactory = require('tests/factory/adminsFactory');
const rolesFactory = require('tests/factory/rolesFactory');

function getAdminsToRolesData(data) {
    return _.assign({}, {
        id: 0,
        adminId: 1,
        roleId: 1
    }, data);
}

function *create(data) {
    const adminsToRolesData = getAdminsToRolesData(data);
    const res = yield db.AdminToRole.findOrCreate({
        where: { id: adminsToRolesData.id },
        defaults: adminsToRolesData
    });

    return res[0];
}

function *createWithRelations(data, relations = {}) {
    const admin = yield adminsFactory.create(relations.admin);
    const role = yield rolesFactory.create(relations.role);

    const adminToRoleData = _.assign({
        adminId: admin.id,
        roleId: role.id
    }, data);

    return yield create(adminToRoleData);
}

module.exports.create = create;
module.exports.createWithRelations = createWithRelations;
