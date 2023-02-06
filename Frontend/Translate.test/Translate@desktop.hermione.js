'use strict';

const PO = require('./Translate.page-object')('desktop');

specs({
    feature: 'Спецсниппет фотоперевода',
}, function() {
    it('Проверка формы загрузки картинки', async function() {
        await this.browser.yaOpenSerp({
            text: 'перевод по фото',
            rearr: 'scheme_Local/PrettySerpFeatures/ForcedFeatures/translate_ocr=1',
            data_filter: 'translate',
        }, PO.translate());

        await this.browser.yaCheckBaobabCounter(PO.translate.dropZone(), {
            path: '/$page/$main/$result/translate-form/upload-image',
        });
    });
});
