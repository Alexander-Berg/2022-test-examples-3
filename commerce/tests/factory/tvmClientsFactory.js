const _ = require('lodash');
const db = require('db/postgres');

function getTvmClientData(data) {
    return _.assign({}, {
        clientId: 1234,
        name: 'clientName'
    }, data);
}

function *create(data) {
    const tvmClientData = getTvmClientData(data);
    const res = yield db.TvmClient.findOrCreate({
        where: { clientId: tvmClientData.clientId },
        defaults: tvmClientData
    });

    return res[0];
}

module.exports.create = create;
