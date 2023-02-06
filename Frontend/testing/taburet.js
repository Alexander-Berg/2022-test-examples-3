// Включаем оптимизации в react
process.env.NODE_ENV = 'production';

const { setup } = require('../../.build/src/vendors/taburet/setup');
const { adapters } = require('../../.build/src/vendors/taburet/adapters');

process.env.YENV = 'testing';

module.exports = {
    taburet: setup({
        adapters
    }),
    getAssetsBundle: require('../common/getAssetsBundle')
};
