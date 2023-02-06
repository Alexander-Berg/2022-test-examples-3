const _ = require('lodash');
const db = require('db/postgres');

function getDirectSyncData(data) {
    return _.assign({}, {
        id: 0,
        startTime: new Date()
    }, data);
}

function *create(data) {
    const directSyncData = getDirectSyncData(data);
    const res = yield db.DirectSync.findOrCreate({
        where: { id: directSyncData.id },
        defaults: directSyncData
    });

    return res[0];
}

module.exports.create = create;
