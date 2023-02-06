const selectors = require('../page-objects');

module.exports = async function ywHideCamerasAndNews({ hideAllCamera = false } = {}) {
    await this.ywHideElems([
        hideAllCamera ? selectors.index.CameraContainer : selectors.index.CameraPlayer,
        selectors.index.NewsNew
    ], { remove: true });
};
