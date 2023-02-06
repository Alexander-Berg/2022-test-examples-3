const _ = require('lodash');
const db = require('db/postgres');
const sectionsFactory = require('tests/factory/sectionsFactory');

function getQuestionData(data) {
    return _.assign({}, {
        id: 0,
        version: 1,
        active: 1,
        archived: 0,
        sectionId: 1,
        categoryId: 1,
        text: 'How many?',
        type: 0
    }, data);
}

function *create(data) {
    const questionData = getQuestionData(data);
    const res = yield db.Question.findOrCreate({
        where: { id: questionData.id, version: questionData.version },
        defaults: questionData
    });

    return res[0];
}

function *createWithRelations(data, relations = {}) {
    const section = yield sectionsFactory.createWithRelations(relations.section, relations);
    const questionData = _.assign({
        sectionId: section.id
    }, data);

    return yield create(questionData);
}

module.exports.create = create;
module.exports.createWithRelations = createWithRelations;
