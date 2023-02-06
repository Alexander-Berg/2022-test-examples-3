const _ = require('lodash');
const db = require('db/postgres');

const questionsFactory = require('tests/factory/questionsFactory');
const trialsFactory = require('tests/factory/trialsFactory');

function getTrialToQuestionsData(data) {
    return _.assign({}, {
        trialId: 1,
        seq: 1,
        questionId: 1,
        questionVersion: 1
    }, data);
}
function *create(data) {
    const trialToQuestionsData = getTrialToQuestionsData(data);
    const res = yield db.TrialToQuestion.findOrCreate({
        where: {
            trialId: trialToQuestionsData.trialId,
            seq: trialToQuestionsData.seq
        },
        defaults: trialToQuestionsData
    });

    return res[0];
}

function *createWithRelations(data, relations = {}) {
    const trial = yield trialsFactory.createWithRelations(relations.trial, relations);
    const question = yield questionsFactory.createWithRelations(relations.question, relations);

    const trialToQuestionData = _.assign({
        trialId: trial.id,
        questionId: question.id,
        questionVersion: question.version
    }, data);

    return yield create(trialToQuestionData);
}

module.exports.create = create;
module.exports.createWithRelations = createWithRelations;
