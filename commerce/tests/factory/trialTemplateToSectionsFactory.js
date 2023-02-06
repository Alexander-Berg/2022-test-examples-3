const _ = require('lodash');
const db = require('db/postgres');

const trialTemplatesFactory = require('tests/factory/trialTemplatesFactory');
const sectionsFactory = require('tests/factory/sectionsFactory');

function getTrialTemplateToSectionData(data) {
    return _.assign({}, {
        id: 0,
        trialTemplateId: 1,
        sectionId: 1,
        categoryId: 1,
        quantity: 2
    }, data);
}

function *create(data) {
    const trialTemplateToSectionData = getTrialTemplateToSectionData(data);
    const res = yield db.TrialTemplateToSection.findOrCreate({
        where: { id: trialTemplateToSectionData.id },
        defaults: trialTemplateToSectionData
    });

    return res[0];
}

function *createWithRelations(data, relations = {}) {
    const trialTemplate = yield trialTemplatesFactory.createWithRelations(relations.trialTemplate, relations);
    const section = yield sectionsFactory.createWithRelations(relations.section, relations);

    const trialTemplateToSectionData = _.assign({
        trialTemplateId: trialTemplate.id,
        sectionId: section.id
    }, data);

    return yield create(trialTemplateToSectionData);
}

module.exports.create = create;
module.exports.createWithRelations = createWithRelations;
