'use strict';

let PO;
const TranslatePO = require('./Translate.page-object');

specs({
    feature: 'Спецсниппет фотоперевода',
}, function() {
    beforeEach(function() {
        PO = TranslatePO(this.currentPlatform);

        return this.browser;
    });

    it('Внешний вид спецсниппета фотоперевода', async function() {
        await this.browser.yaOpenSerp({
            text: 'перевод по фото',
            rearr: 'scheme_Local/PrettySerpFeatures/ForcedFeatures/translate_ocr=1',
            data_filter: 'translate',
        }, PO.translate());

        await this.browser.assertView('ocrTranslate', PO.translate());
    });
});
