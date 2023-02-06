const _ = require('lodash');

const { GlobalUser } = require('db/postgres');

function getGlobalUserData(data) {
    return _.assign({}, {
        id: 1,
        actualLogin: `expert-user-${Date.now()}`,
        isActive: true,
        isBanned: false
    }, data);
}

function *create(data) {
    const globalUserData = getGlobalUserData(data);
    const res = yield GlobalUser.findOrCreate({
        where: { id: globalUserData.id },
        defaults: globalUserData
    });

    return res[0];
}

module.exports.create = create;
