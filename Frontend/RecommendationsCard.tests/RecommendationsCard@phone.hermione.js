specs({
    feature: 'Карточки рекомендаций',
}, () => {
    hermione.only.notIn('safari13');
    it('Внешний вид маленьких карточек', function() {
        return this.browser
            .url('/turbo?stub=recommendationscard%2Fsnippet-cards.json')
            .yaWaitForVisible(PO.pageJsInited(), 'Страница не загрузилась')
            .yaWaitForVisible(PO.recommendationsCard(), 'Карточки не появились')
            .assertView('snippet', PO.recommendationsCardSnippet())
            .assertView('snippet-with-source', PO.recommendationsCardSnippetWithSource());
    });

    hermione.only.notIn('safari13');
    it('Внешний вид маленькой карточки без текста', async function() {
        await this.browser.url('/turbo?stub=recommendationscard%2Fsnippet-cards.json');
        await this.browser.yaWaitForVisible(PO.pageJsInited(), 'Страница не загрузилась');
        await this.browser.yaWaitForVisible(PO.recommendationsCard(), 'Карточки не появились');
        await this.browser.yaIndexify(PO.recommendationsCard());
        await this.browser.assertView('plain', PO.thirdRecommendationsCard());
    });

    hermione.only.notIn('safari13');
    it('Внешний вид маленькой карточки с картинкой между заголовком и аннотацией', async function() {
        await this.browser.url('/turbo?stub=recommendationscard%2Fsnippet-cards.json');
        await this.browser.yaWaitForVisible(PO.pageJsInited(), 'Страница не загрузилась');
        await this.browser.yaWaitForVisible(PO.recommendationsCard(), 'Карточки не появились');
        await this.browser.yaIndexify(PO.recommendationsCard());
        await this.browser.assertView('plain', PO.fourthRecommendationsCard());
    });

    hermione.only.notIn('safari13');
    it('Внешний вид маленькой карточки с картинкой после заголовка и аннотации', async function() {
        await this.browser.url('/turbo?stub=recommendationscard%2Fsnippet-cards.json');
        await this.browser.yaWaitForVisible(PO.pageJsInited(), 'Страница не загрузилась');
        await this.browser.yaWaitForVisible(PO.recommendationsCard(), 'Карточки не появились');
        await this.browser.yaIndexify(PO.recommendationsCard());
        await this.browser.assertView('plain', PO.fifthRecommendationsCard());
    });

    hermione.only.notIn('safari13');
    it('Внешний вид большой карточки', function() {
        return this.browser
            .url('/turbo?stub=recommendationscard%2Flarge-card.json')
            .yaWaitForVisible(PO.pageJsInited(), 'Страница не загрузилась')
            .yaWaitForVisible(PO.recommendationsCard(), 'Карточка не появились')
            .assertView('plain', PO.recommendationsCardLarge());
    });

    /**
     * Из-за того, что рандомизация работает как на сервере, так и на клиенте
     * нет возможности застабить Math.random и скриншоты падают, так что проверяем,
     * что нужный компонент хотя бы рендерится.
     */
    hermione.only.notIn('safari13');
    it('Внешний вид скелетонов', function() {
        return this.browser
            .url('/turbo?stub=recommendationscard%2Fskeletons.json')
            .yaWaitForVisible(PO.pageJsInited(), 'Страница не загрузилась')
            .yaWaitForVisible(PO.recommendationsCard.skeleton(), 'Скелетоны не загрузились');
    });

    hermione.only.notIn('safari13');
    it('Клик по маленькой карточке приводит к переходу на другую страницу', function() {
        return this.browser
            .url('/turbo?stub=recommendationscard%2Fsnippet-cards.json')
            .yaWaitForVisible(PO.pageJsInited(), 'Страница не загрузилась')
            .yaWaitForVisible(PO.recommendationsCard(), 'Карточка не появились')
            .yaCheckLinkOpener(
                PO.recommendationsCard(),
                'Клик по карточке не привел к открытию турбо страницы',
                { target: '_self' }
            );
    });

    hermione.only.notIn('safari13');
    it('Клик по кнопке большой карточки приводит к переходу на другую страницу', function() {
        return this.browser
            .url('/turbo?stub=recommendationscard%2Flarge-card.json')
            .yaWaitForVisible(PO.pageJsInited(), 'Страница не загрузилась')
            .yaWaitForVisible(PO.recommendationsCard(), 'Карточка не появились')
            .yaCheckLinkOpener(
                PO.recommendationsCard.action(),
                'Клик по карточке не привел к открытию турбо страницы',
                { target: '_self' }
            );
    });
});
