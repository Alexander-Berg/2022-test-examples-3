const { testUsers: { externalAdmin, alex } } = hermione.ctx;

describe('Переключение организации (контекста)', () => {
    describe('Положительные', () => {
        describe('Внешний админ', () => {
            it('1. Отображение переключателя организаций на дашборде', function() {
                /* alias: pos-1-view-dashboard */
                return this.browser

                    // залогиниться внешним админом, у которого больше одной организации
                    // перейти на дашборд "{{CONNECT_HOST}}/portal/home"
                    .login({ ...externalAdmin, retpath: '/portal/home' })
                    .disableAnimations('*')

                    // в шапке страницы рядом с логотипом отображается переключатель организаций
                    .isExisting('.header .org-switch')

                    // шапка с переключателем организаций [header]
                    .assertView('header', '.header')

                    // нажать на переключатель организаций
                    .click('.org-switch')

                    // открылся выпадающий список организаций
                    .waitForVisible('.org-switch__popup', 3000)

                    // выпадающий список организаций [dropdown]
                    .assertView('dropdown', '.org-switch__popup');
            });

            it('2. Отображение переключателя организаций на странице ошибки Forbidden "Нет доступа"', function() {
                /* alias: pos-2-view-forbidden */
                return this.browser

                    // залогиниться внешним админом, у которого больше одной организации
                    // перейти на страницу ошибки Forbidden "Нет доступа" "{{CONNECT_HOST}}/portal/forbidden"
                    .login({ ...externalAdmin, retpath: '/portal/forbidden' })
                    .disableAnimations('*')

                    // в шапке страницы рядом с логотипом отображается переключатель организаций
                    .isExisting('.header .organization-switch')

                    // шапка с переключателем организаций [header]
                    .assertView('header', '.header')

                    // нажать на переключатель организаций
                    .click('.organization-switch')

                    // открылся выпадающий список организаций
                    .waitForVisible('.organization-switch__popup', 3000)

                    // выпадающий список организаций [dropdown]
                    .assertView('dropdown', '.organization-switch__popup');
            });

            it('3. Форма создания организации из переключателя организаций', function() {
                /* alias: pos-3-create-org */
                return this.browser

                    // залогиниться внешним админом, у которого больше одной организации
                    // перейти на дашборд "{{CONNECT_HOST}}/portal/home"
                    .login({ ...externalAdmin, retpath: '/portal/home' })
                    .disableAnimations('*')
                    .hideCaret()

                    // нажать на переключатель организаций в шапке страницы рядом с логотипом
                    .click('.org-switch')
                    .waitForVisible('.org-switch__popup', 3000)

                    // в выпадающем меню нажать на пункт "+ Новая организация"
                    .click('.org-switch__popup .org-switch__options .menu__item:first-child')

                    // открылся попап с формой создания новой организации
                    .waitForVisible('.org-switch__add-org-modal .modal__content', 3000)
                    .hideCaret()
                    .disableBorderRadius('.modal__content')

                    // попап с формой создания новой организации [create-org-popup]
                    .assertView('create-org-popup', '.org-switch__add-org-modal .modal__content');
            });
        });
    });

    describe('Отрицательные', () => {
        describe('Внутренний админ', () => {
            it('1. Отображение переключателя организаций', function() {
                /* alias: neg-1-hidden */
                return this.browser

                    // залогиниться внутренним админом
                    // перейти на дашборд "{{CONNECT_HOST}}/portal/home"
                    .login({ ...alex, retpath: '/portal/home' })

                    // переключатель организаций в шапке страницы не отображается
                    .isExisting('.header .org-switch', true);
            });
        });
    });
});
