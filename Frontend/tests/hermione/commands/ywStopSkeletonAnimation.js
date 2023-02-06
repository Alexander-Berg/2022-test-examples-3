const selectors = require('../page-objects');

module.exports = async function ywStopSkeletonAnimation() {
    const { skeletons, tech } = selectors;

    await this.ywHideElems([tech.Spinner, tech.Offline], { remove: true });
    await this.execute(skeletonSelector => {
        [
            ...document.querySelectorAll(skeletonSelector.Text),
            ...document.querySelectorAll(skeletonSelector.Rect),
        ].forEach(item => {
            item.style.animation = 'none';
        });
    }, skeletons);
};
