specs({
    feature: 'SocialBar',
}, () => {
    function testStubs(stubs) {
        stubs.forEach(item => {
            it(`Серые уши ${item.stub}-w${item.width}`, function() {
                return this.browser
                    .windowHandleSize({ width: item.width, height: 1500 })
                    .url(`/turbo?stub=socialbar/${item.stub}.json&exp_flags=commentator-button=conditional&exp_flags=sticky-sociality=1&hermione_commentator=stub`)
                    .yaAssertViewportView('plain');
            });
        });
    }

    it('Проверка видимости контейнера', function() {
        return this.browser
            .windowHandleSize({ width: 1920, height: 1500 })
            .url('/turbo?stub=socialpanel%2Farticle.json&exp_flags=commentator-button=conditional&exp_flags=sticky-sociality=1&hermione_commentator=stub')
            .yaWaitForHidden(PO.blocks.socialBarStickyContainer())
            .yaScrollPageBy(130)
            .yaWaitForVisible(PO.blocks.socialBarStickyContainer(), 'Панель не показалась после скролла на 130px')
            .yaShouldBeVisible(PO.blocks.socialPanel(), 'Липкая панель видна, но плашка комментария не появилась')
            .yaAssertViewportView('wide-screen')
            .windowHandleSize({ width: 1400, height: 1500 })
            .yaAssertViewportView('narrow-screen')
            .yaScrollPageToBottom()
            .yaWaitForHidden(PO.blocks.socialBarStickyContainer(), 'Липкая панель не исчезла при скролле к комментатору');
    });

    it('Логика кнопки комментариев', function() {
        return this.browser
            .windowHandleSize({ width: 1920, height: 1500 })
            .url('/turbo?stub=socialpanel%2Farticle.json&exp_flags=commentator-button=conditional&exp_flags=sticky-sociality=1&hermione_commentator=stub')
            .yaScrollPageBy(130)
            .yaWaitForVisible(PO.blocks.socialButtonComments(), 'Кнопка не показывается при прокручивании 130px')
            .click(PO.blocks.socialButtonComments())
            .yaWaitForVisibleWithinViewport(PO.yandexComments(), 'Скролл до блока комментариев не произошел');
    });

    it('На страницах с брендированием нет кнопок социальности', function() {
        return this.browser
            .windowHandleSize({ width: 1920, height: 1500 })
            .url('/turbo?stub=socialpanel%2Farticle.json&exp_flags=commentator-button=conditional&exp_flags=sticky-sociality=1&exp_flags=branding=1&hermione_commentator=stub')
            .yaShouldNotBeVisible(PO.blocks.socialBar(), 'Панель приехала на верстку')
            .yaShouldNotBeVisible(PO.blocks.body(), 'На странице лишиние серые уши от .body')
            .execute(function() {
                return !document.querySelector('style[data-name="position-announcer"]');
            })
            .then(({ value }) => {
                assert(value, 'На странице присутствует скрипт position-announcer');
            });
    });

    it('Проверка лайков', function() {
        return this.browser
            .windowHandleSize({ width: 1920, height: 1000 })
            .url('/turbo?stub=socialpanel%2Farticle.json&exp_flags=commentator-button=forever&exp_flags=sticky-sociality=1&hermione_commentator=stub')
            .yaShouldBeVisible(PO.page())
            .yaScrollPageBy(130)
            .yaAssertViewportView('with-likes');
    });

    testStubs([
        { stub: 'layout-left', width: 1400 }, // «уши» покажутся
        { stub: 'layout-left', width: 1200 }, // «уши» не покажутся
        { stub: 'layout-all', width: 1700 }, // «уши» покажутся
        { stub: 'layout-all', width: 1400 }, // «уши» не покажутся
        { stub: 'layout-right', width: 1400 }, // «уши» покажутся
        { stub: 'layout-right', width: 1300 }, // «уши» не покажутся
    ]);
});
