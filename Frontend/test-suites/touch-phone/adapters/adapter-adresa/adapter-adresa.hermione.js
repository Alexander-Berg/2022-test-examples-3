'use strict';

const PO = require('../../../../page-objects/touch-phone/index').PO;

specs('Спецсниппет адресный', function() {
    it('одна точка', function() {
        return this.browser
            .yaOpenSerp('text=яндекс такси юридический адрес в москве&p=1')
            .yaWaitForVisible(PO.adresa(), 'Спецсниппет адреса не появился')
            .yaCheckLink(PO.adresa.phoneButton(), { target: '' })
            .then(url => this.browser
                .yaCheckURL(url, 'tel:%2B78463324571',
                    'Сломана ссылка в кнопке телефона', {
                        skipQuery: true,
                        skipPathname: true,
                        skipProtocol: true
                    }
                ))
            .yaMockExternalUrl(PO.adresa.phoneButton())
            .yaCheckBaobabCounter(PO.adresa.phoneButton(), {
                path: '/$page/$main/$result/phone'
            })
            .yaCheckLink(PO.adresa.mapButton())
            .then(url => this.browser
                .yaCheckURL(url, 'http://yandex.ru/maps/?ol=biz&text=yandex-taxi.pro&sspn=&ll=37.687901,55.717589',
                    'Сломана ссылка в кнопке карты', {
                        skipProtocol: true
                    }
                ))
            .yaCheckBaobabCounter(PO.adresa.mapButton(), {
                path: '/$page/$main/$result/map'
            });
    });

    it('несколько точек', function() {
        return this.browser
            .yaOpenSerp('text=шоколадница')
            .yaWaitForVisible(PO.adresa(), 'Спецсниппет адресов не появился')
            .yaCheckLink(PO.adresa.mapButton())
            .then(url => this.browser
                .yaCheckURL(url, 'http://yandex.ru/maps/?ol=biz&text=shoko.ru&sspn=',
                    'Сломана ссылка в кнопке карты', {
                        skipProtocol: true
                    }
                ))
            .yaCheckBaobabCounter(PO.adresa.mapButton(), {
                path: '/$page/$main/$result/map'
            });
    });
});
