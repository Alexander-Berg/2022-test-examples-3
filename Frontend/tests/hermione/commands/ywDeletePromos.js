const { index } = require('../page-objects');

module.exports = async function ywDeletePromos() {
    await this.ywHideElems([index.MobileWidgetPromo], { remove: true });
};
