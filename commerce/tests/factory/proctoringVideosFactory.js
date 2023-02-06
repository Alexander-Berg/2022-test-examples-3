const _ = require('lodash');
const db = require('db/postgres');

const trialsFactory = require('tests/factory/trialsFactory');

function getProctoringVideosData(data) {
    return _.assign({
        trialId: 1,
        name: 'example_video.webm',
        startTime: Date.now(),
        duration: 10000, // 10 секунд
        source: 'webcam'
    }, data);
}

function *create(data) {
    const proctoringVideosData = getProctoringVideosData(data);

    return yield db.ProctoringVideos.findOrCreate({
        where: {
            trialId: proctoringVideosData.trialId,
            name: proctoringVideosData.name
        },
        defaults: proctoringVideosData
    });
}

function *createWithRelations(data, relations = {}) {
    const trial = yield trialsFactory.createWithRelations(relations.trial, relations);
    const proctoringVideosData = _.assign({ trialId: trial.id }, data);

    return yield create(proctoringVideosData);
}

module.exports.create = create;
module.exports.createWithRelations = createWithRelations;
