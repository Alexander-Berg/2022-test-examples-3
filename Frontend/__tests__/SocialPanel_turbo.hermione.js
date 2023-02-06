const url = require('url');

specs({
    feature: 'SocialPanel',
    experiment: 'Со скроллом к виджету комментатора',
}, () => {
    hermione.only.notIn('safari13');
    it('Экспериментальная плашка комментатора с условиями', function() {
        return this.browser
            .url('/turbo?stub=socialpanel%2Farticle.json&exp_flags=commentator-button=conditional&hermione_commentator=stub')
            .yaShouldNotBeVisible(PO.blocks.socialPanelVisible(), 'Плашка появилась в самом начале')
            .yaScrollPageBy(130)
            .yaWaitForVisible(PO.blocks.socialPanelVisible(), 'Плашка не показывается при прокручивании 130px')
            .yaAssertViewportView('top-scroll')
            .click(PO.blocks.socialPanel.button())
            .yaShouldNotBeVisible(PO.blocks.socialPanelVisible(), 'Плашка не исчезла после клика');
    });

    hermione.only.notIn('safari13');
    it('Экспериментальная плашка комментатора всегда', function() {
        return this.browser
            .url('/turbo?stub=socialpanel%2Farticle.json&exp_flags=commentator-button=forever&hermione_commentator=stub')
            .yaWaitForVisible(PO.blocks.socialPanelVisible(), 'Плашка не показывается')
            .yaAssertViewportView('with-start');
    });

    hermione.only.in(['chrome-phone', 'iphone', 'searchapp'], 'Только touch');
    describe('Кнопка копирования ссылки', function() {
        hermione.only.notIn('safari13');
        it('При нажатии в буфер помещается текущий адрес страницы с добавленным utm_source=stick_link_button', function() {
            let copiedUrl;

            return this.browser
                .url('/turbo?stub=socialpanel%2Farticle.json&exp_flags=commentator-button=forever')
                .yaWaitForVisible(PO.blocks.socialPanelVisible(), 'Плашка не показывается')
                .click(PO.blocks.socialPanel.copyLinkButton())
                .yaGetClipboardText()
                .then(result => {
                    copiedUrl = url.parse(result, true);
                    assert.property(
                        copiedUrl.query,
                        'utm_source',
                        'Скопированная ссылка не содержит CGI-параметр "utm_source"'
                    );
                    assert.equal(
                        copiedUrl.query.utm_source,
                        'stick_link_button',
                        'Значение CGI-параметра "utm_source" в скопированной ссылке не "stick_link_button"'
                    );
                })
                .getUrl()
                .then(currentUrl => {
                    const {
                        utm_source,
                        ...queryWithoutUtmSource
                    } = copiedUrl.query;
                    copiedUrl.query = queryWithoutUtmSource;
                    return this.browser.yaCheckURL(copiedUrl, currentUrl, 'Ссылка в буфере обмена неправильная');
                })
                .yaWaitForHidden(PO.blocks.socialPanel.tooltip(), 3000, 'Попап "Текст скопирован" не спрятался');
        });

        hermione.only.notIn('safari13');
        it('По нажатию отправляется статистика', function() {
            return this.browser
                .url('/turbo?stub=socialpanel%2Farticle.json&exp_flags=commentator-button=forever')
                .yaWaitForVisible(PO.blocks.socialPanelVisible(), 'Плашка не показывается')
                .yaCheckBaobabCounter(PO.blocks.socialPanel.copyLinkButton(), {
                    path: '$page',
                    event: 'tech',
                    type: 'stick-link-button-click',
                }, { message: 'Статистика не отправилась' });
        });
    });
    describe('Проверка работы лайков', function() {
        hermione.only.notIn('safari13');
        it('Лайк с реакцией пользователя', function() {
            return this.browser
                .url('/turbo?stub=socialpanel%2Flikes-true.json&exp_flags=commentator-button=forever&exp_flags=sticky-sociality=1&hermione_commentator=stub')
                .yaShouldBeVisible(PO.page())
                .yaAssertViewportView('like-true');
        });
        hermione.only.notIn('safari13');
        it('Лайк без реакции пользователя', function() {
            return this.browser
                .url('/turbo?stub=socialpanel%2Farticle.json&exp_flags=commentator-button=forever&exp_flags=sticky-sociality=1&hermione_commentator=stub')
                .yaShouldBeVisible(PO.page())
                .yaAssertViewportView('like-false');
        });
    });
});
