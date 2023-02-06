const PO = require('./UniSearchEducation.page-object/index@common');

async function openPreview(_this, serpParams, action) {
    const defaultAction = async () =>
        _this.browser.click(PO.UniSearchEducation.Content.List.Item());
    action = action || defaultAction;
    await _this.browser.yaOpenSerp(serpParams, PO.UniSearchEducation());

    await _this.browser.execute(function(sel) {
        const list = document.querySelector(sel);
        const links = list.querySelectorAll('a');

        for (let i = 0; i < links.length; i++) {
            links[i].href = '#';
        }
    }, [PO.UniSearchEducation.Content.List()]);

    await _this.browser.yaRetryActionsByElemVisible(
        2,
        PO.UniSearchEducationPreview(),
        action,
        3000);
}

module.exports = {
    openPreview,
};
