const _ = require('lodash');
const db = require('db/postgres');

const adminsFactory = require('tests/factory/adminsFactory');
const trialTemplatesFactory = require('tests/factory/trialTemplatesFactory');
const globalUsersFactory = require('tests/factory/globalUsersFactory');

function getBanData(data) {
    return _.assign({}, {
        globalUserId: 1,
        adminId: 2,
        trialTemplateId: 1,
        action: 'ban',
        reason: 'some reason',
        startedDate: new Date(),
        userLogin: 'banned-login',
        isLast: false
    }, data);
}

function *create(data) {
    const banData = getBanData(data);

    return yield db.Ban.create(banData);
}

function *createWithRelations(data, relations = {}) {
    const globalUser = yield globalUsersFactory.create(relations.globalUser);
    const admin = yield adminsFactory.create(relations.admin);
    const trialTemplate = yield trialTemplatesFactory.createWithRelations(relations.trialTemplate, relations);

    const banData = _.assign({
        globalUserId: globalUser.id,
        adminId: admin.id,
        trialTemplateId: trialTemplate.id
    }, data);

    return yield create(banData);
}

module.exports.create = create;
module.exports.createWithRelations = createWithRelations;
