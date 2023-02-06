specs({
    feature: 'SocialButtons',
    experiment: 'Круглые залипающие кнопки на тачах',
}, () => {
    hermione.only.notIn('safari13');
    it('Отображение залипающих кнопок с подсказками', function() {
        return this.browser
            .url('/turbo?stub=socialbuttons%2Fpage.json&hermione_commentator=stub&exp_flags=social-buttons=1')
            .yaWaitForVisible(PO.blocks.socialButtons(), 'Кнопки не появились при загрузке страницы')
            .yaAssertViewportView('plain')
            .yaScrollPageToBottom()
            .yaScrollPageBy(-100)
            .pause(1000)
            .yaWaitForVisible(PO.blocks.socialButtons.buttonExpanded(), 'Кнопка не развернулась после скролла и паузы')
            .yaAssertViewportView('expanded-comment')
            .yaWaitForHidden(PO.blocks.socialButtons.buttonExpanded(), 'Кнопка не свернулась обратно')
            .yaScrollPageBy(300)
            .pause(1000)
            .yaScrollPageToBottom()
            .yaWaitForVisible(PO.blocks.socialButtons.buttonExpanded(), 'Вторая кнопка не развернулась после скролла и паузы')
            .yaAssertViewportView('expanded-like');
    });

    hermione.only.notIn('safari13');
    it('Отображается сообщение после нажатия на "Скопировать ссылку"', function() {
        return this.browser
            .url('/turbo?stub=socialbuttons%2Fpage.json&hermione_commentator=stub&exp_flags=social-buttons=1&exp_flags=social-buttons-toast-show-time=4000')
            .yaWaitForVisible(PO.blocks.socialButtons.buttonCopyLink(), 'Кнопка "Скопировать ссылку" не появилась')
            .click(PO.blocks.socialButtons.buttonCopyLink())
            .yaWaitForVisible(PO.blocks.toastVisible())
            .yaAssertViewportView('toast');
    });

    hermione.only.notIn('safari13');
    it('Взаимодействие кнопок с залипающей плашкой рекламы', function() {
        return this.browser
            .url('/turbo?stub=socialbuttons%2Fpage-sticky-advert.json&hermione_commentator=stub&exp_flags=social-buttons=1&exp_flags=social-buttons-blue=1&exp_flags=advert-bottom-min-height=400&exp_flags=social-buttons-time-before-expand=10&exp_flags=adv-disabled=0&hermione_advert=stub')
            .yaWaitForVisible(PO.blocks.socialButtons(), 'Кнопки не появились при загрузке страницы')
            .yaScrollPageBy(400)
            .yaWaitForVisible(PO.blocks.advertBottomVisible())
            .yaAssertViewportView('with-advert')
            .click(PO.blocks.advertBottomClose())
            .yaWaitForHidden(PO.blocks.advertBottomVisible())
            .yaScrollPageBy(600)
            .yaAssertViewportView('without-advert');
    });

    hermione.only.notIn('safari13');
    it('При перезагрузке страницы кнопка не разворачивается повторно', function() {
        return this.browser
            .url('/turbo?stub=socialbuttons%2Fpage.json&hermione_commentator=stub&exp_flags=social-buttons=1&exp_flags=social-buttons-time-before-expand=10')
            .yaWaitForVisible(PO.blocks.socialButtons(), 'Кнопки не появились при загрузке страницы')
            .yaScrollPageToBottom()
            .yaWaitForVisible(PO.blocks.socialButtons.buttonCommentsExpanded())
            .url('/turbo?stub=socialbuttons%2Fpage.json&hermione_commentator=stub&exp_flags=social-buttons=1&exp_flags=social-buttons-time-before-expand=10')
            .yaWaitForVisible(PO.blocks.socialButtons(), 'Кнопки не появились при повторном открытии страницы')
            .yaScrollPageToBottom()
            .yaWaitForVisible(PO.blocks.socialButtons.buttonLikeExpanded())
            .url('/turbo?stub=socialbuttons%2Fpage.json&hermione_commentator=stub&exp_flags=social-buttons=1&exp_flags=social-buttons-time-before-expand=10')
            .yaWaitForVisible(PO.blocks.socialButtons(), 'Кнопки не появились при третьем открытии страницы')
            .yaScrollPageToBottom()
            .yaWaitForVisible(PO.blocks.socialButtons.buttonCopyLinkExpanded());
    });
});
