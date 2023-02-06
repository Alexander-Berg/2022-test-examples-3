const _ = require('lodash');
const db = require('db/postgres');

const trialTemplatesFactory = require('tests/factory/trialTemplatesFactory');
const sectionsFactory = require('tests/factory/sectionsFactory');

function getTrialTemplateAllowedFailsData(data) {
    return _.assign({}, {
        id: 0,
        trialTemplateId: 1,
        sectionId: 1,
        allowedFails: 1
    }, data);
}

function *create(data) {
    const trialTemplateAllowedFailsData = getTrialTemplateAllowedFailsData(data);
    const res = yield db.TrialTemplateAllowedFails.findOrCreate({
        where: { id: trialTemplateAllowedFailsData.id },
        defaults: trialTemplateAllowedFailsData
    });

    return res[0];
}

function *createWithRelations(data, relations = {}) {
    const trialTemplate = yield trialTemplatesFactory.createWithRelations(relations.trialTemplate, relations);
    const section = yield sectionsFactory.createWithRelations(relations.section, relations);

    const trialTemplateAllowedFailsData = _.assign({
        trialTemplateId: trialTemplate.id,
        sectionId: section.id
    }, data);

    return yield create(trialTemplateAllowedFailsData);
}

module.exports.create = create;
module.exports.createWithRelations = createWithRelations;
