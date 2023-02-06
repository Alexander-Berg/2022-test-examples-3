const { Entity, ReactEntity } = require('../../../../vendors/hermione');
const commonElems = require('./index@common');

const elems = commonElems();

elems.seventhSerpItem = new Entity('.serp-item[data-cid="7"] .organic__title-wrapper');

elems.translate.sourceLangSwitch.text = new ReactEntity({ block: 'Button2', elem: 'Text' });
elems.translate.sourceLangSwitch.control = new ReactEntity({ block: 'Select2', elem: 'Control' });
elems.translate.sourceLangSwitch.firstItemNative = new Entity('option').nthChild(1);
elems.translate.clear = new ReactEntity({ block: 'Translate', elem: 'Clear' });
elems.translate.suggestItem = elems.translate.suggest.nthChild(1);

elems.translate.targetLangSwitch.text = new ReactEntity({ block: 'Button2', elem: 'Text' });
elems.translate.targetLangSwitch.control = new ReactEntity({ block: 'Select2', elem: 'Control' });
elems.translate.targetLangSwitch.firstItemNative = new Entity('option').nthChild(1);
elems.translate.targetLangSwitch.AzerbajaniLangNative = new Entity('option').nthChild(1);

elems.translate.speakerPopupSource = new ReactEntity({ block: 'Translate', elem: 'SpeakerPopup' }).mods({ direction: 'source' });
elems.translate.speakerPopupTarget = new ReactEntity({ block: 'Translate', elem: 'SpeakerPopup' }).mods({ direction: 'target' });

module.exports = elems;
