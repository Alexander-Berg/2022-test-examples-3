const { ReactEntity } = require('../../../../../../vendors/hermione');
const { UniSearchPreview: UniSearchPreviewCommon } = require('./index@common');

const UniSearchPreview = UniSearchPreviewCommon.copy();
UniSearchPreview.Wrapper = new ReactEntity({ block: 'Drawer', elem: 'Curtain' });
UniSearchPreview.Overlay = new ReactEntity({ block: 'Drawer', elem: 'Overlay' });
UniSearchPreview.Content = new ReactEntity({ block: 'Drawer', elem: 'Content' });

module.exports = {
    UniSearchPreview,
};
