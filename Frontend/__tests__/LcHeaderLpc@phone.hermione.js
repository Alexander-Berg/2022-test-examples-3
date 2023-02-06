specs({
    feature: 'LcHeaderLpc',
}, () => {
    hermione.only.notIn('safari13');
    it('User', function() {
        var userPopupVisible = '.legouser__popup.light-popup_visible_yes';

        return this.browser
            .url('/turbo?stub=lcheaderlpc/user.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaShouldNotBeVisible(userPopupVisible, 'Попап юзера изначально должен быть закрыт')
            .assertView('plain', PO.lcHeaderLpc())
            .touch(PO.lcHeaderLpc.userWrapperMobile())
            .yaWaitForVisible(userPopupVisible, 'Попап юзера не открылся')
            .assertView('popup-opened', userPopupVisible);
    });

    hermione.skip.in('iphone', 'Тест нестабилен');
    describe('Скролл по якорям', function() {
        hermione.only.notIn('safari13');
        it('Учитывает высоту закрепленной шапки', function() {
            const ctaSelector = `.lc-header-lpc__fixed-wrapper ${PO.lcHeaderLpc.callToActionMobile()}`;

            return this.browser
                .url('/turbo?stub=lcheaderlpc/fixed-smooth-scroll.json')
                .yaWaitForVisible(ctaSelector, 'Кнопка не загрузилась')
                .touch(ctaSelector)
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

        hermione.only.notIn('safari13');
        it('Игнорирует высоту незакрепленной шапки', function() {
            return this.browser
                .url('/turbo?stub=lcheaderlpc/smooth-scroll.json')
                .yaWaitForVisible(PO.lcHeaderLpc.callToActionMobile(), 'Кнопка не загрузилась')
                .touch(PO.lcHeaderLpc.callToActionMobile())
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
    });

    describe('Бургер', function() {
        hermione.only.notIn('safari13');
        it('Кнопка', function() {
            return this.browser
                .url('/turbo?stub=lcheaderlpc/burger-menu.json')
                .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
                .assertView('button', PO.lcHeaderLpc());
        });

        hermione.only.notIn('safari13');
        it('Меню', function() {
            return this.browser
                .url('/turbo?stub=lcheaderlpc/burger-menu.json')
                .yaWaitForVisible(PO.lcHeaderLpc.burgerWrapper(), 'Кнопка не загрузилась')
                .touch(PO.lcHeaderLpc.burgerWrapper())
                .assertView('menu', PO.lcHeaderLpc.burgerMenu(), { screenshotDelay: 500 });
        });
    });
});
