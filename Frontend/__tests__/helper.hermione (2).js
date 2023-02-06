const PO = require('../../../../hermione/page-objects');

function checkBlockView() {
    return this.browser
        .yaWaitForVisible(PO.talkSocialPanel())
        .assertView('panel', PO.talkSocialPanel());
}

function checkReactionSetting(buttonSelector, resultViewName) {
    return this.browser
        .yaWaitForVisible(PO.talkSocialPanel())
        .click(buttonSelector)
        .yaWaitForVisible(PO.likeLight.iconPressed())
        .assertView(resultViewName, PO.talkSocialPanel());
}

function checkScrollToComments() {
    return this.browser
        .yaWaitForVisible(PO.talkSocialPanel())
        .click(PO.talkSocialPanel.commentsButton())
        .yaWaitForVisible(PO.yandexComments(), 'Скролл до блока комментариев не произошел');
}

module.exports = {
    checkBlockView,
    checkReactionSetting,
    checkScrollToComments,
};
