'use strict';

const PO = require('../../../../page-objects/touch-phone/index').PO;

specs('Гео-колдунщик, одна организация', function() {
    it('Базовые проверки', function() {
        return this.browser
            .yaOpenSerp('text=кафе+пушкин')
            .waitForVisible(PO.companies(), 'Гео-колдунщик не появился')
            .yaHideElement(PO.smartBanner())

            .yaCheckLink(PO.companies1orgContainer.title()).then(url => this.browser
                .yaCheckURL(url, 'http://yandex.ru/profile/1018907821', 'Сломана ссылка на тайтле', {
                    skipProtocol: true,
                    skipQuery: true
                }))
            .yaCheckBaobabCounter(PO.companies1orgContainer.title(), {
                path: '/$page/$main/$result[@wizard_name="companies"]/title'
            })

            .yaCheckLink(PO.companies1orgContainer.thumb()).then(url => this.browser
                .yaCheckURL(url, 'http://yandex.ru/maps/', 'Сломана ссылка на тумбе', {
                    skipProtocol: true,
                    skipQuery: true
                }))
            .yaCheckBaobabCounter(PO.companies1orgContainer.thumb(), {
                path: '/$page/$main/$result[@wizard_name="companies"]/thumb'
            })

            .yaCheckLink(PO.companies1orgContainer.map()).then(url => this.browser
                .yaCheckURL(url, 'http://yandex.ru/profile/1018907821', 'Сломана ссылка на карте', {
                    skipProtocol: true,
                    skipQuery: true
                }))
            .yaCheckBaobabCounter(PO.companies1orgContainer.map(), {
                path: '/$page/$main/$result[@wizard_name="companies"]/map'
            })

            .yaCheckLink(PO.companies1orgContainer.greenurl())
            .yaCheckBaobabCounter(PO.companies1orgContainer.greenurl(), {
                path: '/$page/$main/$result[@wizard_name="companies"]/site'
            })

            .yaCheckLink(PO.companies1orgContainer.address()).then(url => this.browser
                .yaCheckURL(url, 'http://yandex.ru/profile/1018907821', 'Сломана ссылка на адресе', {
                    skipProtocol: true,
                    skipQuery: true
                }))
            .yaCheckBaobabCounter(PO.companies1orgContainer.address(), {
                path: '/$page/$main/$result[@wizard_name="companies"]/address'
            })

            .yaCheckLink(PO.companies1orgContainer.phone(), { target: '' }).then(url =>
                assert.equal(url.protocol, 'tel:', 'Ссылка на телефонном номере без протокола tel:')
            )
            .yaMockExternalUrl(PO.companies1orgContainer.phone())
            .yaCheckBaobabCounter(PO.companies1orgContainer.phone(), {
                path: '/$page/$main/$result[@wizard_name="companies"]/phone'
            })

            .yaCheckLink(PO.companies1orgContainer.ratingStars()).then(url => this.browser
                .yaCheckURL(url, 'http://yandex.ru/maps/org/', 'Сломана ссылка на рейтинге', {
                    skipProtocol: true,
                    skipPathnameTrail: true,
                    skipQuery: true
                }))
            .yaCheckBaobabCounter(PO.companies1orgContainer.ratingStars(), {
                path: '/$page/$main/$result[@wizard_name="companies"]/rating'
            })

            .yaCheckLink(PO.companies1orgContainer.ratingReviews()).then(url => this.browser
                .yaCheckURL(url, 'http://yandex.ru/maps/org/', 'Сломана ссылка на отзывах', {
                    skipProtocol: true,
                    skipPathnameTrail: true,
                    skipQuery: true
                }))
            .yaCheckBaobabCounter(PO.companies1orgContainer.ratingReviews(), {
                path: '/$page/$main/$result[@wizard_name="companies"]/rating'
            });
    });
});
