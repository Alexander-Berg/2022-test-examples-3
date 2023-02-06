const _ = require('lodash');
const db = require('db/postgres');

const adminsFactory = require('tests/factory/adminsFactory');
const trialTemplatesFactory = require('tests/factory/trialTemplatesFactory');

function getLocksData(data) {
    return _.assign({}, {
        id: 0,
        adminId: 1,
        trialTemplateId: 1,
        lockDate: new Date()
    }, data);
}

function *create(data) {
    const locksData = getLocksData(data);
    const res = yield db.Lock.findOrCreate({
        where: { id: locksData.id },
        defaults: locksData
    });

    return res[0];
}

function *createWithRelations(data, relations = {}) {
    const admin = yield adminsFactory.create(relations.admin);
    const trialTemplate = yield trialTemplatesFactory.createWithRelations(relations.trialTemplate, relations);

    const lockData = _.assign({
        adminId: admin.id,
        trialTemplateId: trialTemplate.id
    }, data);

    return yield create(lockData);
}

module.exports.create = create;
module.exports.createWithRelations = createWithRelations;
