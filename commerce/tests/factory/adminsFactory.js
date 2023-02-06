const _ = require('lodash');
const db = require('db/postgres');

function getAdminData(data) {
    return _.assign({}, {
        id: 0,
        uid: 9876543210987,
        login: 'expert-admin'
    }, data);
}

function *create(data) {
    const adminData = getAdminData(data);
    const res = yield db.Admin.findOrCreate({
        where: { id: adminData.id },
        defaults: adminData
    });

    return res[0];
}

module.exports.create = create;
