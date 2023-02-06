const preview = require('../../../../UniSearch.components/Preview/Preview.test/Preview.page-object/index@touch-phone');
const { drawer } = require('../../../../../../components/Drawer/Drawer.test/Drawer.page-object/index@touch-phone');
const common = require('./index@common');

module.exports = {
    ...common,
    ...preview,
    Drawer: drawer,
};
