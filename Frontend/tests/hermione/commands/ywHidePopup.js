const selectors = require('../page-objects');

module.exports = async function ywHidePopup() {
    /**
     * нельзя использовать e.remove(), иначе взорверся реакт в e2e тесте
     * при попытке обращения к DOM удаленного компонента
     */
    await this.ywHideElems([selectors.popup], { remove: false });
};
