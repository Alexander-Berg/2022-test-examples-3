const _ = require('lodash');

const db = require('db/postgres');
const trialTemplatesFactory = require('tests/factory/trialTemplatesFactory');
const adminsFactory = require('tests/factory/adminsFactory');

function getDraftData(data) {
    return _.assign({}, {
        trialTemplateId: 1,
        adminId: 1,
        saveDate: new Date(),
        status: 'saved'
    }, data);
}

function *create(data) {
    const draftData = getDraftData(data);

    const res = yield db.Draft.findOrCreate({
        where: { id: draftData.id },
        defaults: draftData
    });

    return res[0];
}

function *createWithRelations(data, relations = {}) {
    const trialTemplate = yield trialTemplatesFactory.createWithRelations(relations.trialTemplate, relations);
    const admin = yield adminsFactory.create(relations.admin);

    const draftData = _.assign({
        trialTemplateId: trialTemplate.id,
        adminId: admin.id
    }, data);

    return yield create(draftData);
}

module.exports.create = create;
module.exports.createWithRelations = createWithRelations;
