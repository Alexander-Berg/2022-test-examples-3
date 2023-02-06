const popups = require('../page-objects/client-popups');
const { photo } = require('../page-objects/client-photo2-page').common;
const slider = require('../page-objects/slider');

hermione.skip.in('chrome-phone-6.0', 'Артефакты при снятии скриншотов');
describe('Предупреждением про копирование из безлимита ->', () => {
    beforeEach(async function() {
        const bro = this.browser;
        await bro.yaClientLoginFast('yndx-ufo-test-101');
        await bro.yaOpenSection('photo');
        await bro.yaWaitForVisible(photo.item.preview());
        await bro.click(photo.item());
        await bro.yaWaitForVisible(slider.common.contentSlider.previewImage());
        await bro.click(slider.common.sliderButtons.moreButton());
    });

    it('diskclient-4439, 4322: assertView: Показ предупреждения в окне копирования', async function() {
        const bro = this.browser;

        await bro.yaWaitForVisible(popups.common.actionBarMorePopup.copyButton());
        await bro.click(popups.common.actionBarMorePopup.copyButton());
        await bro.yaWaitForVisible(popups.common.selectFolderDialog.treeContent());
        await bro.yaSetModalDisplay(popups.common.selectFolderDialog());
        await bro.assertView('diskclient-4439', popups.common.selectFolderPopup.content());

        await bro.click(popups.common.selectFolderPopup.warningTooltip.hide());
        await bro.yaWaitForHidden(popups.common.selectFolderPopup.warningTooltip());
        await bro.assertView('diskclient-4439-hidden', popups.common.selectFolderPopup.content());
    });

    it('diskclient-5072, 4282: Показ предупреждения в окне перемещения', async function() {
        const bro = this.browser;

        await bro.yaWaitForVisible(popups.common.actionBarMorePopup.moveButton());
        await bro.click(popups.common.actionBarMorePopup.moveButton());
        await bro.yaWaitForVisible(popups.common.selectFolderDialog.treeContent());
        await bro.yaSetModalDisplay(popups.common.selectFolderDialog());
        await bro.assertView('diskclient-4282', popups.common.selectFolderPopup.content());

        await bro.click(popups.common.selectFolderPopup.warningTooltip.hide());
        await bro.yaWaitForHidden(popups.common.selectFolderPopup.warningTooltip());
        await bro.assertView('diskclient-4282-hidden', popups.common.selectFolderPopup.content());
    });
});
