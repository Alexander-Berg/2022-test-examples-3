const _ = require('lodash');
const db = require('db/postgres');

function getServiceData(data) {
    return _.assign({}, {
        id: 0,
        code: 'direct',
        title: 'Yandex.Direct'
    }, data);
}

function *create(data) {
    const serviceData = getServiceData(data);

    const res = yield db.Service.findOrCreate({
        where: { id: serviceData.id },
        defaults: serviceData
    });

    return res[0];
}

module.exports.create = create;
