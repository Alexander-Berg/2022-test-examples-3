const db = require('db/index');

module.exports = () => {
    return db.sequelize.queryInterface.createSchema('events');
};
