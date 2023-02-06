const _ = require('lodash');
const db = require('db/postgres');
const questionsFactory = require('tests/factory/questionsFactory');

function getAnswerData(data) {
    return _.assign({}, {
        id: 0,
        questionId: 1,
        questionVersion: 1,
        correct: 0,
        text: 'Infinity times',
        active: 1
    }, data);
}

function *create(data) {
    const answerData = getAnswerData(data);
    const res = yield db.Answer.findOrCreate({
        where: { id: answerData.id },
        defaults: answerData
    });

    return res[0];
}

function *createWithRelations(data, relations = {}) {
    const question = yield questionsFactory.createWithRelations(relations.question, relations);
    const answerData = _.assign({
        questionId: question.id,
        questionVersion: question.version
    }, data);

    return yield create(answerData);
}

module.exports.create = create;
module.exports.createWithRelations = createWithRelations;
