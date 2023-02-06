'use strict';

const PO = require('../../../../page-objects/touch-phone/index').PO;

specs('Объектный ответ', function() {
    beforeEach(function() {
        return this.browser
            .yaOpenSerp('text=мадонна')
            .yaWaitForVisible(PO.entityCard(), 'Объектный ответ отсутствует в выдаче');
    });

    it('Проверка счетчиков и ссылок', function() {
        return this.browser
            .yaCheckLink(PO.entityCard.titleLink())
            .yaCheckBaobabCounter(PO.entityCard.titleLink(), {
                path: '/$page/$main/$result[@wizard_name="entity_search"]/title'
            })
            .yaCheckLink(PO.entityCard.subtitleLink())
            .yaCheckBaobabCounter(PO.entityCard.subtitleLink(), {
                path: '/$page/$main/$result[@wizard_name="entity_search"]/subtitle'
            })
            .yaCheckLink(PO.entityCard.thumb()).then(url =>
                this.browser.yaCheckURL(url, 'https://yandex.ru/images', 'Сломана ссылка на тумбе', {
                    skipQuery: true,
                    skipPathname: true
                })
            )
            .yaCheckBaobabCounter(PO.entityCard.thumb(), {
                path: '/$page/$main/$result[@wizard_name="entity_search"]/thumb'
            })
            .yaCheckLink(PO.entityCard.firstFactLink(), { target: '' })
            .yaMockExternalUrl(PO.entityCard.firstFactLink())
            .yaCheckBaobabCounter(PO.entityCard.firstFactLink(), {
                path: '/$page/$main/$result[@wizard_name="entity_search"]/"wikifact/p0"'
            })
            .yaCheckLink(PO.entityCard.secondFactLink(), { target: '' })
            .yaMockExternalUrl(PO.entityCard.secondFactLink())
            .yaCheckBaobabCounter(PO.entityCard.secondFactLink(), {
                path: '/$page/$main/$result[@wizard_name="entity_search"]/"wikifact/p1"'
            })
            .yaCheckLink(PO.entityCard.firstFooterLink())
            .yaCheckBaobabCounter(PO.entityCard.firstFooterLink(), {
                path: '/$page/$main/$result[@wizard_name="entity_search"]/p0'
            })
            .yaCheckLink(PO.entityCard.secondFooterLink())
            .yaCheckBaobabCounter(PO.entityCard.secondFooterLink(), {
                path: '/$page/$main/$result[@wizard_name="entity_search"]/p1'
            })
            .yaCheckBaobabCounter(PO.entityCard.cutLink(), {
                path: '/$page/$main/$result[@wizard_name="entity_search"]/cut'
            });
    });

    it('Проверка блока ката', function() {
        return this.browser
            .click(PO.entityCard.cutLink())
            .yaWaitForVisible(PO.entityCard.cutInvisible(), 'Кат не раскрылся');
    });
});
