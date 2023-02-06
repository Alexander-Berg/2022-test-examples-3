const _ = require('lodash');
const db = require('db/postgres');
const rolesFactory = require('tests/factory/rolesFactory');
const agenciesFactory = require('tests/factory/agenciesFactory');
const authTypesFactory = require('tests/factory/authTypesFactory');
const globalUsersFactory = require('tests/factory/globalUsersFactory');

function getUserData(data) {
    return _.assign({}, {
        id: 2345,
        login: 'expert-user',
        firstname: 'Expert',
        lastname: 'Expertov',
        roleId: 1,
        agencyId: 1,
        authTypeId: 0,
        uid: 12345678901,
        yandexUid: null
    }, data);
}

function *create(data) {
    const userData = getUserData(data);
    const res = yield db.User.findOrCreate({
        where: { id: userData.id },
        defaults: userData
    });

    return res[0];
}

function *createWithRelations(data, relations = {}) {
    const role = yield rolesFactory.create(relations.role, relations);
    const agency = yield agenciesFactory.create(relations.agency);
    const authType = yield authTypesFactory.create(relations.authType);
    const userData = _.assign({ roleId: role.id, agencyId: agency.id, authTypeId: authType.id }, data);

    if (relations.globalUser) {
        const globalUser = yield globalUsersFactory.create(relations.globalUser);

        userData.globalUserId = globalUser.id;
    }

    return yield create(userData);
}

module.exports.create = create;
module.exports.createWithRelations = createWithRelations;
