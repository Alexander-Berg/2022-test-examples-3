specs({
    feature: 'LcHeaderLpc',
}, () => {
    it('User', function() {
        var userPopupVisible = '.legouser__popup.light-popup_visible_yes';

        return this.browser
            .url('/turbo?stub=lcheaderlpc/user.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaShouldNotBeVisible(userPopupVisible, 'Попап юзера изначально должен быть закрыт')
            .assertView('plain', PO.lcHeaderLpc())
            .click(PO.lcHeaderLpc.userWrapperDesktop())
            .yaWaitForVisible(userPopupVisible, 'Попап юзера не открылся')
            .moveToObject('.legouser__menu-header .legouser__account')
            .assertView('popup-opened', userPopupVisible, {
                screenshotDelay: 3000,
            });
    });

    describe('Скролл по якорям', function() {
        it('Учитывает высоту закрепленной шапки', function() {
            const ctaSelector = `.lc-header-lpc__fixed-wrapper ${PO.lcHeaderLpc.callToActionDesktop()}`;

            return this.browser
                .url('/turbo?stub=lcheaderlpc/fixed-smooth-scroll.json')
                .yaWaitForVisible(ctaSelector, 'Кнопка не загрузилась')
                .click(ctaSelector)
                .execute(function() {
                    var section = document.querySelector('#tekst-2');
                    var header = document.querySelector('.lc-header-lpc__fixed-wrapper');

                    if (!section || !header) {
                        return false;
                    }

                    return {
                        sectionTop: Math.floor(section.getBoundingClientRect().top),
                        headerHeight: header.clientHeight,
                    };
                })
                .then(result => {
                    assert.isNotFalse(result.value, 'Шапка или секция не найдены на странице');

                    const { sectionTop, headerHeight } = result.value;

                    assert.strictEqual(sectionTop, headerHeight, 'Неправильная позиция после скролла');
                });
        });

        it('Игнорирует высоту незакрепленной шапки', function() {
            return this.browser
                .url('/turbo?stub=lcheaderlpc/smooth-scroll.json')
                .yaWaitForVisible(PO.lcHeaderLpc.callToActionDesktop(), 'Кнопка не загрузилась')
                .click(PO.lcHeaderLpc.callToActionDesktop())
                .execute(function() {
                    var section = document.querySelector('#tekst-2');

                    if (!section) {
                        return false;
                    }

                    return Math.floor(section.getBoundingClientRect().top);
                })
                .then(result => {
                    assert.isNotFalse(result.value, 'Секция не найдена на странице');
                    assert.strictEqual(result.value, 0, 'Неправильная позиция после скролла');
                });
        });

        it('Скролл до последней секции', function() {
            return this.browser
                .url('/turbo?stub=lcheaderlpc/fixed-smooth-scroll.json')
                .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
                .yaScrollPageToBottom()
                .pause(1000)
                .assertView('fixedScrollLast', ['.lc-header-lpc__fixed-wrapper']);
        });
    });
});
