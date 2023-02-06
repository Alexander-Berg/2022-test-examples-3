const _ = require('lodash');
const db = require('db/postgres');

function getTypeData(data) {
    return _.assign({}, {
        id: 0,
        code: 'cert',
        title: 'Certification'
    }, data);
}

function *create(data) {
    const typeData = yield getTypeData(data);
    const res = yield db.Type.findOrCreate({
        where: { id: typeData.id },
        defaults: typeData
    });

    return res[0];
}

module.exports.create = create;
