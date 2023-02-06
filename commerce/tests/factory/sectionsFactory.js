const _ = require('lodash');
const db = require('db/postgres');

const servicesFactory = require('tests/factory/servicesFactory');

function getSectionData(data) {
    return _.assign({}, {
        id: 0,
        serviceId: 1,
        code: 'rules',
        title: 'Price rules'
    }, data);
}

function *create(data) {
    const sectionData = getSectionData(data);
    const query = { id: sectionData.id };

    const res = yield db.Section.findOrCreate({
        where: query,
        defaults: sectionData
    });

    return res[0];
}

function *createWithRelations(data, relations = {}) {
    const service = yield servicesFactory.create(relations.service, relations);

    const sectionData = _.assign({ serviceId: service.id }, data);

    return yield create(sectionData);
}

module.exports.create = create;
module.exports.createWithRelations = createWithRelations;
