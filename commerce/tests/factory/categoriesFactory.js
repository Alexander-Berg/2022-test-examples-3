const _ = require('lodash');
const db = require('db/postgres');

function getCategoryData(data) {
    return _.assign({}, {
        id: 0,
        difficulty: 0,
        timeLimit: 10
    }, data);
}

function *create(data) {
    const categoryData = getCategoryData(data);
    const res = yield db.Category.findOrCreate({
        where: { id: categoryData.id },
        defaults: categoryData
    });

    return res[0];
}

module.exports.create = create;
