const _ = require('lodash');
const db = require('db/postgres');

const trialsFactory = require('tests/factory/trialsFactory');

function getProctoringResponsesData(data) {
    return _.assign({}, {
        trialId: 1,
        verdict: 'failed',
        source: 'proctoring',
        time: new Date(),
        isLast: false,
        isSentToToloka: false,
        isRevisionRequested: false
    }, data);
}

function *create(data) {
    const proctoringResponsesData = getProctoringResponsesData(data);

    return yield db.ProctoringResponses.create(proctoringResponsesData);
}

function *createWithRelations(data, relations = {}) {
    const trial = yield trialsFactory.createWithRelations(relations.trial, relations);
    const proctoringResponsesData = _.assign({ trialId: trial.id }, data);

    return yield create(proctoringResponsesData);
}

module.exports.create = create;
module.exports.createWithRelations = createWithRelations;
