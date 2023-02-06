'use strict';

const PO = require('../../page-objects/desktop').PO;
const YABS_COUNTER = 'http://yabs.yandex.ru/count/';

specs('Спец.размещение', function() {
    const pathSitelinkCounter = '/$page/$main/$result[@type="adv"]/sitelinks/item';

    hermione.only.notIn('ie8', 'nth-of-type не работает в ie8-');
    it('Проверка ссылок и счетчиков', function() {
        return this.browser
            .yaOpenSerp({ foreverdata: '2128511162', exp_flags: 'hide-popups=1' })
            .yaWaitForVisible(PO.adv(), 'Рекламный сниппет не появился')
            .yaCheckLink(PO.adv.title.link()).then(url => this.browser
                .yaCheckURL(url, YABS_COUNTER, 'Сломана ссылка в тайтле', {
                    skipProtocol: true,
                    skipPathnameTrail: true,
                    skipQuery: true
                })
            )
            .yaCheckLink(PO.adv.path.item()).then(url => this.browser
                .yaCheckURL(url, YABS_COUNTER, 'Сломана ссылка в гринурле', {
                    skipProtocol: true,
                    skipPathnameTrail: true,
                    skipQuery: true
                })
            )
            .yaCheckBaobabCounter(PO.adv.sitelinks.first(), { path: pathSitelinkCounter })
            .yaCheckLink(PO.adv.sitelinks.first()).then(url => this.browser
                .yaCheckURL(url, YABS_COUNTER, 'Сломана ссылка в первом сайтлинке', {
                    skipProtocol: true,
                    skipPathnameTrail: true,
                    skipQuery: true
                })
            )
            .yaCheckLink(PO.adv.meta.contacts()).then(url => this.browser
                .yaCheckURL(url, YABS_COUNTER, 'Сломана ссылка "Контактная информация"', {
                    skipProtocol: true,
                    skipPathnameTrail: true,
                    skipQuery: true
                })
            );
    });

    it('Показ при опечатке', function() {
        return this.browser
            .yaOpenSerp({ text: 'оккна', lr: 213 })
            .yaWaitForVisible(PO.adv(), 'Рекламный сниппет не появился');
    });

    it('Проверка блока с информацией про отели', function() {
        return this.browser
            .yaOpenSerp({
                foreverdata: 773523505
            })
            .assertView('plain', PO.adv());
    });
});
