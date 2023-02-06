specs({
    feature: 'sandwich-menu',
    experiment: 'На react',
}, () => {
    describe('Стандартное меню', () => {
        beforeEach(function() {
            return this.browser
                .url('/turbo?stub=sandwichmenu/default-react.json&exp_flags=force-react-sandwich-menu=1')
                .yaWaitForVisible(PO.turboSandwichMenu());
        });

        hermione.only.notIn('safari13');
        it('Открытие и закрытие меню', function() {
            return this.browser
                .assertView('button-open', PO.turboSandwichMenu())
                .click(PO.turboSandwichMenu.handler())
                .yaWaitForVisible(PO.turboSandwichMenuContainer(), 'Меню должно раскрыться')
                .assertView('menu', PO.turboSandwichMenuContainer())
                .click(PO.turboSandwichMenuContainer.closeButton())
                .yaWaitForHidden(PO.turboSandwichMenuContainer(), 'Меню должно закрыться по нажатию на оверлей')
                .click(PO.turboSandwichMenu.handler())
                .yaWaitForVisible(PO.turboSandwichMenuContainer(), 'Меню должно раскрыться')
                .click(PO.turboSandwichMenuContainer.closeIcon())
                .yaWaitForHidden(PO.turboSandwichMenuContainer(), 'Меню должно закрыться по нажатию на иконку');
        });

        hermione.only.notIn('safari13');
        it('Ссылка внутри меню', function() {
            return this.browser
                .click(PO.turboSandwichMenu.handler())
                .yaWaitForVisible(PO.turboSandwichMenuContainer(), 'Меню должно раскрыться')
                .click(PO.turboSandwichMenuContainer.content.link());
        });

        hermione.only.notIn('safari13');
        it('Возможность скроллить меню', function() {
            return this.browser
                .click(PO.turboSandwichMenu.handler())
                .yaWaitForVisible(PO.turboSandwichMenuContainer(), 'Меню должно раскрыться')
                .yaShouldBeScrollable(PO.turboSandwichMenuContainer.contentWrap(), { v: true, h: false });
        });

        hermione.only.in('chrome-phone');
        hermione.only.notIn('safari13');
        it('Отсутствие скролла по кнопке', function() {
            let initScroll;

            return this.browser
                .click(PO.turboSandwichMenu.handler())
                .yaWaitForVisible(PO.turboSandwichMenuContainer(), 'Меню должно раскрыться')
                .yaTouchScroll(PO.turboSandwichMenuContainer.closeButton(), 0, 100)
                .execute(() =>
                    document.documentElement.scrollTop || document.body.scrollTop
                )
                .then(({ value }) => assert.strictEqual(value, 0, 'Страница проскролилась по кнопке'))
                // Слишком далеко тянуть не надо, иначе выполнится жест
                // вытаскивания спиннера перезагрузки страницы в браузере и сработает перезагрузка.
                .yaTouchScroll(PO.turboSandwichMenuContainer.contentWrap(), 0, -100)
                .execute(() =>
                    document.documentElement.scrollTop || document.body.scrollTop
                )
                .then(({ value }) => assert.strictEqual(value, 0, 'Страница проскролилась по меню'))
                .execute(() => {
                    const menuContainer = document.querySelector('.turbo-sandwich-menu-container__content-wrap');
                    return menuContainer.scrollHeight - menuContainer.clientHeight;
                })
                .then(({ value }) =>
                    this.browser.yaTouchScroll(PO.turboSandwichMenuContainer.contentWrap(), 0, value)
                )
                .execute(() =>
                    document.querySelector('.turbo-sandwich-menu-container__content-wrap').scrollTop
                )
                .then(({ value }) => {
                    initScroll = value;
                    assert.notEqual(value, 0, 'Меню не проскроллилось');
                })
                .yaTouchScroll(PO.turboSandwichMenuContainer.contentWrap(), 0, 100)
                .execute(() =>
                    document.querySelector('.turbo-sandwich-menu-container__content-wrap').scrollTop
                )
                .then(({ value }) => {
                    assert.strictEqual(value, initScroll, 'Меню скроллится за свои пределы');
                });
        });
    });

    describe('Меню с якорями', () => {
        hermione.only.notIn('safari13');
        it('Скрывает меню по клику на якорь', function() {
            return this.browser
                .url('/turbo?stub=sandwichmenu%2Fanchor-react.json&exp_flags=force-react-sandwich-menu=1')
                .yaWaitForVisible(PO.turboSandwichMenu())
                .click(PO.turboSandwichMenu.handler())
                .yaWaitForVisible(PO.turboSandwichMenuContainer(), 'Меню должно раскрыться')
                .click(PO.turboSandwichMenuContainer.link())
                .yaWaitForHidden(PO.turboSandwichMenuContainer(), 'Меню должно скрыться')
                .yaWaitForVisibleWithinViewport(PO.linkLike(), 'Страница должна проскроллиться к якорю');
        });
    });

    describe('Меню с вложенными элементами', () => {
        beforeEach(function() {
            return this.browser
                .url('/turbo?stub=sandwichmenu/deep-react.json&exp_flags=force-react-sandwich-menu=1')
                .yaWaitForVisible(PO.turboSandwichMenu())
                .click(PO.turboSandwichMenu.handler())
                .yaWaitForVisible(PO.turboSandwichMenuContainer(), 'Меню должно раскрыться');
        });

        hermione.only.notIn('safari13');
        it('Внешний вид', function() {
            return this.browser
                .assertView('extended', PO.turboSandwichMenuContainer());
        });

        hermione.only.notIn('safari13');
        it('Открытие и закрытие спойлера', function() {
            return this.browser
                .click(PO.turboSandwichMenuContainer.content.linkWrap.accordion.title())
                .yaWaitForVisible(PO.turboSandwichMenuContainer.content.linkWrap.accordion.content(), 'Спойлер не раскрылся')
                .assertView('accordion-expanded', PO.turboSandwichMenuContainer.content.linkWrap.accordion())
                .click(PO.turboSandwichMenuContainer.content.linkWrap.accordion.accordion.title())
                .yaWaitForVisible(PO.turboSandwichMenuContainer.content.linkWrap.accordion.accordion.content(), 'Спойлер не раскрылся')
                .assertView('accordion-expanded-inner', PO.turboSandwichMenuContainer.content.linkWrap.accordion())
                .click(PO.turboSandwichMenuContainer.content.linkWrap.accordion.accordion.title())
                .yaWaitForHidden(PO.turboSandwichMenuContainer.content.linkWrap.accordion.accordion.content(), 'Спойлер не закрылся')
                .click(PO.turboSandwichMenuContainer.content.linkWrap.accordion.title())
                .yaWaitForHidden(PO.turboSandwichMenuContainer.content.linkWrap.accordion.content(), 'Спойлер не закрылся');
        });

        hermione.only.in('chrome-phone', 'setOrientation() используем только в chrome-phone');
        hermione.only.notIn('safari13');
        it('Внешний вид в горизонтальной ориентации', function() {
            return this.browser
                .setOrientation('landscape')
                .assertView('extended-horizontal', PO.turboSandwichMenuContainer());
        });
    });
});
