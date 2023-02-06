'use strict';

const PO = require('../../../../page-objects/touch-phone/index').PO;

specs('Видео / Порно', function() {
    beforeEach(function() {
        return this.browser
            .yaOpenSerp('text=порно видео')
            .yaWaitForVisible(PO.videoWizard(), 'Должен появиться колдунщик видео');
    });

    it('Проверка наличия блюра', function() {
        return this.browser
            .getAttribute('.t-construct-adapter__video .thumb__image', 'src')
            .then(attrs => {
                assert(attrs.every(attr => (attr || '').includes('shower=7')));
            });
    });
});
