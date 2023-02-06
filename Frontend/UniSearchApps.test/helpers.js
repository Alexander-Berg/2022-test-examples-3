const PO = require('./UniSearchApps.page-object/index');

async function openPreview(_this, serpParams, action) {
    const defaultAction = async () => _this.browser.click(PO.UniSearchApps.Content.List.Item());

    action = action || defaultAction;
    await _this.browser.yaOpenSerp(serpParams, PO.UniSearchApps());

    await _this.browser.execute(function(sel) {
        const list = document.querySelector(sel);
        const links = list.querySelectorAll('a');

        for (let i = 0; i < links.length; i++) {
            links[i].href = '#';
        }
    }, [PO.UniSearchApps.Content.List()]);

    await _this.browser.yaRetryActionsByElemVisible(
        2,
        PO.UniSearchAppsPreview(),
        action,
        3000);
}

module.exports = {
    openPreview,
};
