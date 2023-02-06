const _ = require('lodash');
const db = require('db/postgres');
const trialTemplatesFactory = require('tests/factory/trialTemplatesFactory');
const usersFactory = require('tests/factory/usersFactory');

function getTrialData(data) {
    return _.assign({}, {
        id: 0,
        userId: 374627,
        trialTemplateId: 1,
        sequenceNumber: 1,
        questionCount: 1,
        allowedFails: 1,
        timeLimit: 1
    }, data);
}

function *create(data) {
    const trialData = getTrialData(data);
    const res = yield db.Trial.findOrCreate({
        where: { id: trialData.id },
        defaults: trialData
    });

    return res[0];
}

function *createWithRelations(data, relations = {}) {
    const trialTemplate = yield trialTemplatesFactory.createWithRelations(relations.trialTemplate, relations);
    const user = yield usersFactory.createWithRelations(relations.user, relations);

    const trialData = _.assign({
        trialTemplateId: trialTemplate.id,
        userId: user.id
    }, data);

    return yield create(trialData);
}

module.exports.create = create;
module.exports.createWithRelations = createWithRelations;
