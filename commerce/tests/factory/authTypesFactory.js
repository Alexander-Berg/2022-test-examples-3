const _ = require('lodash');
const db = require('db/postgres');

function getAuthTypeData(data) {
    return _.assign({}, {
        id: 0,
        code: 'www'
    }, data);
}

function *create(data) {
    const authTypeData = getAuthTypeData(data);
    const res = yield db.AuthType.findOrCreate({
        where: { id: authTypeData.id },
        defaults: authTypeData
    });

    return res[0];
}

module.exports.create = create;
