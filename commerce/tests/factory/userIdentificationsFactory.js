const _ = require('lodash');
const db = require('db/postgres');
const trialTemplatesFactory = require('tests/factory/trialTemplatesFactory');
const usersFactory = require('tests/factory/usersFactory');

function getUserIdentificationData(data) {
    return _.assign({}, {
        userId: 0,
        trialTemplateId: 1,
        document: 'path_to_document',
        face: 'path_to_face'
    }, data);
}

function *create(data) {
    const userIdentificationData = getUserIdentificationData(data);
    const res = yield db.UserIdentification.findOrCreate({
        where: {
            userId: userIdentificationData.userId,
            trialTemplateId: userIdentificationData.trialTemplateId,
            face: userIdentificationData.face,
            document: userIdentificationData.document
        },
        defaults: userIdentificationData
    });

    return res[0];
}

function *createWithRelations(data, relations = {}) {
    const trialTemplate = yield trialTemplatesFactory.createWithRelations(relations.trialTemplate, relations);
    const user = yield usersFactory.createWithRelations(relations.user, relations);

    const userIdentificationData = _.assign({
        trialTemplateId: trialTemplate.id,
        userId: user.id
    }, data);

    return yield create(userIdentificationData);
}

module.exports.create = create;
module.exports.createWithRelations = createWithRelations;
