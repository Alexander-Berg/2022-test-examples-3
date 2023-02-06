specs({
    feature: 'LcCards(phone)',
}, () => {
    hermione.only.notIn('safari13');
    it('Обычные карточки', function() {
        return this.browser
            .url('/turbo?stub=lccards/default.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('phone-default', PO.lcCards());
    });

    hermione.only.notIn('safari13');
    it('Карточки без указания ширины', function() {
        return this.browser
            .url('/turbo?stub=lccards/without-width-s3.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('phone-without-width-s3', PO.lcCards());
    });

    hermione.only.notIn('safari13');
    it('Карточки без указания ширины (аватарница)', function() {
        return this.browser
            .url('/turbo?stub=lccards/without-width.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('phone-without-width', PO.lcCards());
    });

    hermione.only.notIn('safari13');
    it('Карточки с кнопкой CTA', function() {
        return this.browser
            .url('/turbo?stub=lccards/ctaButton.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('phone-button', PO.lcCards())
            .touch(PO.lcCards.cardInner())
            .assertView('phone-button-hover', PO.lcCards());
    });

    hermione.only.notIn('safari13');
    it('Карточки с ссылкой CTA', function() {
        return this.browser
            .url('/turbo?stub=lccards/ctaLink.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('phone-link', PO.lcCards())
            .touch(PO.lcCards.cardInner())
            .assertView('phone-link-hover', PO.lcCards());
    });

    hermione.only.notIn('safari13');
    it('Карточки с модальным окном', function() {
        return this.browser
            .url('/turbo?stub=lccards/ctaModal.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('phone-with-hidden-modal', PO.lcCards())
            .touch(PO.lcCards.cta())
            .assertView('phone-with-visible-modal', PO.lcCardsModal());
    });
});
