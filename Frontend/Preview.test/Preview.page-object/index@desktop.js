const { ReactEntity } = require('../../../../../../vendors/hermione');
const { UniSearchPreview: UniSearchPreviewCommon } = require('./index@common');

const UniSearchPreview = UniSearchPreviewCommon.copy();
UniSearchPreview.Wrapper = new ReactEntity({ block: 'Modal', elem: 'Wrapper' });
UniSearchPreview.Close = new ReactEntity({ block: 'UniSearchPreview', elem: 'Close' });
UniSearchPreview.Overlay = new ReactEntity({ block: 'Modal', elem: 'Overlay' });

module.exports = {
    UniSearchPreview,
};
