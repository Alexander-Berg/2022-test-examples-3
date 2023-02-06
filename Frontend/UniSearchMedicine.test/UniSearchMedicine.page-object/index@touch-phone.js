const { UniSearchPreview } = require('../../../../UniSearch.components/Preview/Preview.test/Preview.page-object/index@touch-phone');
const { drawer } = require('../../../../../../components/Drawer/Drawer.test/Drawer.page-object/index@touch-phone');
const PO = require('./index@common');

module.exports = {
    ...PO,
    UniSearchPreview,
    Drawer: drawer,
};
