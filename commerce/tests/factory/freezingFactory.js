const _ = require('lodash');
const db = require('db/postgres');

const trialTemplatesFactory = require('tests/factory/trialTemplatesFactory');

function getFreezingData(data) {
    return _.assign({}, {
        id: 0,
        frozenBy: 1234567890,
        startTime: new Date(),
        finishTime: new Date(),
        trialTemplateId: 1
    }, data);
}

function *create(data) {
    const freezingData = getFreezingData(data);
    const res = yield db.Freezing.findOrCreate({
        where: { id: freezingData.id },
        defaults: freezingData
    });

    return res[0];
}

function *createWithRelations(data, relations = {}) {
    const trialTemplate = yield trialTemplatesFactory.createWithRelations(relations.trialTemplate, relations);

    const freezingData = _.assign({
        trialTemplateId: trialTemplate.id
    }, data);

    return yield create(freezingData);
}

module.exports.create = create;
module.exports.createWithRelations = createWithRelations;
