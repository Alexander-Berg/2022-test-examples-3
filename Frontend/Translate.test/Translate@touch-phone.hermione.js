'use strict';

const PO = require('./Translate.page-object')('touch-phone');

specs({
    feature: 'Спецсниппет фотоперевода',
}, function() {
    it('Проверка формы загрузки картинки', async function() {
        await this.browser.yaOpenSerp({
            text: 'перевод по фото',
            rearr: 'scheme_Local/PrettySerpFeatures/ForcedFeatures/translate_ocr=1',
            data_filter: 'translate',
        }, PO.translate());

        await this.browser.yaCheckBaobabCounter(PO.translate.camera(), {
            path: '/$page/$main/$result/translate-form/camera',
        });

        await this.browser.yaCheckBaobabCounter(PO.translate.gallery(), {
            path: '/$page/$main/$result/translate-form/gallery',
        });
    });
});
