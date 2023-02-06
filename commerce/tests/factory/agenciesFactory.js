const _ = require('lodash');
const db = require('db/postgres');

function getAgencyData(data) {
    return _.assign({}, {
        id: 0,
        login: 'pupkin-agency',
        title: 'i-pupkin',
        manager: 'yndx-pupkin',
        directId: 9876
    }, data);
}

function *create(data) {
    const agencyData = getAgencyData(data);
    const res = yield db.Agency.findOrCreate({
        where: { id: agencyData.id },
        defaults: agencyData
    });

    return res[0];
}

module.exports.create = create;
