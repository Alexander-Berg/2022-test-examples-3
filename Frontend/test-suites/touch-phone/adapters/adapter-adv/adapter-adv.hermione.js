'use strict';

const PO = require('../../../../page-objects/touch-phone/index').PO;

specs('Спец.размещение', function() {
    beforeEach(function() {
        return this.browser
            .yaOpenSerp(
                {
                    text: 'аренда офисов',
                    foreverdata: '2209802523'
                }
            )
            .yaWaitForVisible(PO.serpList.serpAdvItem(), 'рекламный сниппет должен присутствовать в выдаче');
    });

    it('Проверка ссылок и счётчиков', function() {
        const ADV_URL = {
            href: 'http://yabs.yandex.ru/count',
            ignore: ['protocol', 'pathname_trail', 'query']
        };

        return this.browser
            .yaCheckSnippet(PO.serpList.serpAdvItem, {
                title: { url: ADV_URL },
                greenurl: [{ url: ADV_URL }],
                sitelinks: [{
                    selector: PO.serpList.serpAdvItem.firstLink(),
                    url: ADV_URL
                }]
            })
            .yaCheckBaobabServerCounter({
                path: '/$page/$main/$result[@type="adv" and @externalId@entity="banner"]'
            });
    });

    it('Проверка телефонной трубки', function() {
        return this.browser
            .yaCheckLink(PO.serpList.serpAdvItem.phoneButton(), {
                target: '',
                checkClickability: false
            })
            .then(url => this.browser
                .yaCheckURL(
                    url,
                    'tel:%2B375296749180%2C9',
                    'Сломана ссылка в кнопке телефона'
                )
            )
            .yaCheckBaobabCounter(
                PO.serpList.serpAdvItem.phoneButton(),
                { path: '/$page/$main/$result[@type="adv"]/button' }
            );
    });

    hermione.only.notIn(['iphone', 'winphone'], 'orientation is not supported');
    it('Поворот экрана', function() {
        return this.browser
            .orientation('landscape')
            .yaShouldBeVisible(PO.serpList.serpAdvItem());
    });
});
