const selectors = require('../page-objects');

module.exports = async function ywDeleteLoader() {
    await this.ywHideElems([selectors.tech.Spinner]);
};
