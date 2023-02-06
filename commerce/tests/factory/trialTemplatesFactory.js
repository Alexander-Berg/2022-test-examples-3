const _ = require('lodash');
const db = require('db/postgres');

const typesFactory = require('tests/factory/typesFactory');
const servicesFactory = require('tests/factory/servicesFactory');

function getTrialTemplateData(data) {
    return _.assign({}, {
        id: 0,
        active: 1,
        serviceId: 1,
        typeId: 1,
        title: 'Testing test',
        description: 'Description for test',
        rules: 'list of rules',
        seoDescription: '',
        ogDescription: '',
        previewCert: 'url to preview',
        allowedTriesCount: 50,
        timeLimit: 900000,
        allowedFails: 1,
        delays: '1M, 1M, 1M, 1M, 1M, 1M, 1M, 1M, 1M, 1M, 1M, 1M, 1M, 1M',
        validityPeriod: '3M',
        periodBeforeCertificateReset: '1M',
        delayUntilTrialsReset: '1M',
        language: 0,
        isProctoring: false,
        clusterSlug: 'test'
    }, data);
}

function *create(data) {
    const trialTemplateData = getTrialTemplateData(data);
    const res = yield db.TrialTemplate.findOrCreate({
        where: { id: trialTemplateData.id },
        defaults: trialTemplateData
    });

    return res[0];
}

function *createWithRelations(data, relations = {}) {
    const type = yield typesFactory.create(relations.type, relations);
    const service = yield servicesFactory.create(relations.service, relations);

    const trialTemplateData = _.assign({
        typeId: type.id,
        serviceId: service.id
    }, data);

    return yield create(trialTemplateData);
}

module.exports.create = create;
module.exports.createWithRelations = createWithRelations;
