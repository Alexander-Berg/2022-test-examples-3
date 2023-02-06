specs({
    feature: 'LcCards(desktop)',
}, () => {
    it('Обычные карточки', function() {
        return this.browser
            .url('/turbo?stub=lccards/default.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('desktop-default', PO.lcCards());
    });

    it('Карточки без указания ширины', function() {
        return this.browser
            .url('/turbo?stub=lccards/without-width-s3.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('desktop-without-width-s3', PO.lcCards());
    });

    it('Карточки без указания ширины (аватарница)', function() {
        return this.browser
            .url('/turbo?stub=lccards/without-width.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('desktop-without-width', PO.lcCards());
    });

    it('Карточки с кнопкой CTA', function() {
        return this.browser
            .url('/turbo?stub=lccards/ctaButton.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('desktop-button', PO.lcCards())
            .moveToObject(PO.lcCards.cardInner())
            .assertView('desktop-button-hover', PO.lcCards());
    });

    it('Карточки с ссылкой CTA', function() {
        return this.browser
            .url('/turbo?stub=lccards/ctaLink.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('desktop-link', PO.lcCards())
            .moveToObject(PO.lcCards.cardInner())
            .assertView('desktop-link-hover', PO.lcCards());
    });

    it('Карточки с модальным окном', function() {
        return this.browser
            .url('/turbo?stub=lccards/ctaModal.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('desktop-with-hidden-modal', PO.lcCards())
            .click(PO.lcCards.cta())
            .assertView('desktop-with-visible-modal', PO.lcCardsModal());
    });
});
