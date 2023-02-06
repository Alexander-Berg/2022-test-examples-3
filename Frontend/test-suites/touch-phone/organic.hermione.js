'use strict';

const PO = require('../../page-objects/touch-phone/index').PO;

specs('Базовая функциональность (Органика)', function() {
    it('Проверка ссылок', function() {
        return this.browser
            .yaOpenSerp('text=stackoverflow')
            .yaWaitForVisible(PO.organic(), 'Должен присутствовать сниппет органики')
            .yaCheckSnippet(PO.organic, {
                title: {},
                greenurl: [{}],
                sitelinks: [{ selector: PO.organic.sitelinks.first() }]
            });
    });

    it('Проверка гринурла', function() {
        return this.browser
            .yaOpenSerp('text=караси+в+аквариуме')
            .yaWaitForVisible(PO.organic(), 'Должен присутствовать сниппет органики')
            .elements(PO.organic.path.sequentiveItem()).then(items =>
                assert.lengthOf(items.value, 0, 'В гринурле более одной ссылки')
            )
            .elements(PO.organic.path.separator()).then(separators =>
                assert.lengthOf(separators.value, 0, 'В гринурле присутствует разделитель')
            );
    });
});
