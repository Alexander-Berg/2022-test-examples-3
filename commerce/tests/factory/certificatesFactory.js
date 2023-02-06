const _ = require('lodash');
const db = require('db/postgres');
const trialsFactory = require('tests/factory/trialsFactory');
const typesFactory = require('tests/factory/typesFactory');

function getCertificateData(data) {
    return _.assign({}, {
        id: 0,
        trialId: 1,
        typeId: 1,
        firstname: 'Ivan',
        lastname: 'Ivanov',
        dueDate: new Date(),
        imagePath: '255/38472434872_13'
    }, data);
}

function *create(data) {
    const certificateData = getCertificateData(data);
    const res = yield db.Certificate.findOrCreate({
        where: { id: certificateData.id },
        defaults: certificateData
    });

    return res[0];
}

function *createWithRelations(data, relations = {}) {
    const trial = yield trialsFactory.createWithRelations(relations.trial, relations);
    const type = yield typesFactory.create(relations.type, relations);
    const certificateData = _.assign({
        trialId: trial.id,
        typeId: type.id
    }, data);

    return yield create(certificateData);
}

module.exports.create = create;
module.exports.createWithRelations = createWithRelations;
