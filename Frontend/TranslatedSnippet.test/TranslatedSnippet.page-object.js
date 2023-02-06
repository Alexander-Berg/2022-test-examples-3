'use strict';

const { ReactEntity, create } = require('../../../vendors/hermione/index');
const { extralinksPopup } = require('../../../components/Extralinks/Extralinks.test/Extralinks.page-object/index');
const { organic } = require('../../../components/SerpOrganic/SerpOrganic.test/SerpOrganic.page-object/index');
const { popup } = require('../../../components/Popup/Popup.test/Popup.page-object/index@common');

const PO = {};

PO.popup = popup.copy();
PO.extralinksPopup = extralinksPopup.copy();
PO.verifiedBlue = new ReactEntity({ block: 'Verified', elem: 'Icon' });
PO.verifiedTooltip = new ReactEntity({ block: 'Verified', elem: 'Tooltip' });
PO.translatedSnippet = organic.mix(new ReactEntity({ block: 'TranslatedSnippet' }));
// оригинальный заголовок определен внутри вспомогательного компонента OrganicLikeFooter
PO.translatedSnippet.title = new ReactEntity({ block: 'TranslatedSnippet', elem: 'Footer' })
    .descendant(organic.title.copy());
// гринурл определен внутри вспомогательного компонента OrganicLikeFooter
PO.translatedSnippet.path = new ReactEntity({ block: 'TranslatedSnippet', elem: 'Footer' })
    .descendant(organic.greenurl.copy());
PO.translatedSnippet.extralinks = organic.extralinks.copy();
PO.translatedSnippet.label = new ReactEntity({ block: 'TranslatedSnippet', elem: 'Label' });
PO.translatedSnippet.translatedTitle = new ReactEntity({ block: 'TranslatedSnippet', elem: 'TranslatedTitle' });
PO.translatedSnippet.translatedText = new ReactEntity({ block: 'TranslatedSnippet', elem: 'TranslatedText' });
PO.translatedSnippet.translatedPath = organic.greenurl
    .copy()
    .mix(new ReactEntity({ block: 'TranslatedSnippet', elem: 'TranslatedPath' }));

module.exports = create(PO);
