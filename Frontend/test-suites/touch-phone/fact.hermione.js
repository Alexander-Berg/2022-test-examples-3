'use strict';

const PO = require('../../page-objects/touch-phone/index').PO;

specs({
    feature: 'Факт'
}, function() {
    it('entity-fact', function() {
        return this.browser
            .yaOpenSerp('text=сколько лет жириновскому')
            .yaWaitForVisible(PO.fact(), 'Фактовый ответ не появился')
            .yaCheckLink(PO.fact.question.link(), { target: '' })
            .yaCheckBaobabCounter(PO.fact.question.link(), {
                path: '/$page/$main/$result[@wizard_name="entity-fact"]/question'
            });
    });

    hermione.only.notIn('winphone', 'winphone не поддерживает target: _blank у ссылок');
    it('suggest-fact', function() {
        return this.browser
            .yaOpenSerp({
                text: 'когда начинают резаться зубки',
                exp_flags: 'extended-preview-disable'
            })
            .yaWaitForVisible(PO.fact(), 'Фактовый ответ не появился')
            .yaCheckLink(PO.fact.title.link())
            .yaCheckBaobabCounter(PO.fact.title.link(), {
                path: '/$page/$main/$result[@wizard_name="suggest_fact"]/title'
            })
            .yaCheckLink(PO.fact.path.link())
            .yaCheckBaobabCounter(PO.fact.path.link(), {
                path: '/$page/$main/$result[@wizard_name="suggest_fact"]/path/urlnav'
            });
    });

    hermione.only.notIn('winphone', 'winphone не поддерживает target: _blank у ссылок');
    it('suggest-fact с турбо', function() {
        return this.browser
            .yaOpenSerp({
                text: 'что такое прокурор'
            })
            .yaWaitForVisible(PO.fact(), 'Фактовый ответ не появился')
            .yaCheckLink(PO.fact.title.link())
            .yaCheckBaobabCounter(PO.fact.title.link(), {
                path: '/$page/$main/$result[@wizard_name="suggest_fact"]/title'
            })
            .yaCheckLink(PO.fact.path.link())
            .yaCheckBaobabCounter(PO.fact.path.link(), {
                path: '/$page/$main/$result[@wizard_name="suggest_fact"]/path/urlnav'
            });
    });

    it('С телефоном', function() {
        return this.browser
            .yaOpenSerp('text=телефон сбербанка')
            .yaWaitForVisible(PO.fact(), 'Фактовый ответ не появился')
            .yaCheckLink(PO.fact.phone(), { target: '' }).then(url =>
                this.browser.yaCheckURL(url, 'tel:900', 'Неверная ссылка в кнопке телефона')
            )
            .yaCheckBaobabCounter(PO.fact.phone(), {
                path: '/$page/$main/$result[@wizard_name="suggest_fact"]/button'
            });
    });

    hermione.only.notIn('winphone', 'winphone не поддерживает target: _blank у ссылок');
    it('Калории', function() {
        return this.browser
            .yaOpenSerp('text=апельсин калория')
            .yaWaitForVisible(PO.fact(), 'Фактовый ответ не появился')
            .yaWaitForVisible(PO.objectFooter(), 'Источник данных не появился')
            .yaCheckLink(PO.fact.question.link(), { target: '' })
            .yaMockExternalUrl(PO.fact.question.link())
            .yaCheckBaobabCounter(PO.fact.question.link(), {
                path: '/$page/$main/$result[@wizard_name="calories_fact"]/question'
            })
            .yaCheckLink(PO.objectFooter.link())
            .yaCheckBaobabCounter(PO.objectFooter.link(), {
                path: '/$page/$main/$result[@wizard_name="calories_fact"]/p0'
            });
    });

    it('Психологическая помощь', function() {
        const path = '/$page/$main/$result';

        return this.browser
            .yaOpenSerp('foreverdata=1615237189')
            .yaWaitForVisible(PO.fact(), 'Фактовый ответ не появился')
            .yaCheckLink(PO.fact.description.link(), { target: '' }).then(url =>
                this.browser.yaCheckURL(url, 'tel:88001004994', 'Неверная ссылка в кнопке телефона')
            )
            .yaMockExternalUrl(PO.fact.title.link())
            .yaCheckBaobabCounter(
                PO.fact.title.link(),
                {
                    path: `${path}/title`
                }
            )
            .yaMockExternalUrl(PO.fact.path.link())
            .yaCheckBaobabCounter(
                PO.fact.path.link(),
                {
                    path: `${path}/path/urlnav`
                }
            )
            .yaMockExternalUrl(PO.fact.description.link())
            .yaCheckBaobabCounter(
                PO.fact.description.link(),
                {
                    path: `${path}/phone`
                }
            );
    });

    it('Психологическая помощь - проверка серверных счетчиков', function() {
        const path = '/$page/$main/$result';

        return this.browser
            .yaOpenSerp('foreverdata=1615237189')
            .yaWaitForVisible(PO.fact(), 'Фактовый ответ не появился')
            .yaCheckBaobabServerCounter({
                path: `${path}/title`
            })
            .yaCheckBaobabServerCounter({
                path: `${path}/path/urlnav`
            })
            .yaCheckBaobabServerCounter({
                path: `${path}/phone`
            });
    });

    it('Расстояние между объектами', function() {
        return this.browser
            .yaOpenSerp('text=расстояние от москвы до питера')
            .yaWaitForVisible(PO.fact(), 'Фактовый ответ не появился')
            .getText(PO.fact.question()).then(value => assert.equal(value, 'Москва — Санкт-Петербург'))
            .getText(PO.fact.answer()).then(value => assert.equal(value, '634 км (по прямой)'));
    });
});
