import {makeSuite, makeCase} from 'ginny';
import MenuCatalog from '@self/platform/spec/page-objects/components/MenuCatalog';

/**
 * Тесты на виджет BubbleCategories
 * @param {PageObject.BubbleCategories} bubbleCategories
 */
export default makeSuite('Виджет категории кружочками.', {
    environment: 'testing',
    story: {
        beforeEach() {
            this.setPageObjects({
                menuCatalog: () => this.createPageObject(MenuCatalog),
            });
        },
        'По умолчанию': {
            'должен отображаться': makeCase({
                id: 'm-touch-1929',
                issue: 'MOBMARKET-7713',
                test() {
                    return this.browser.allure.runStep('Проверям видимость виджета', () =>
                        this.bubbleCategories.isVisible()
                            .should.eventually.to.be.equal(true, 'Виджет отображается')
                    );
                },
            }),
        },
        'По клику на "все категории"': {
            'раскрывается каталог': makeCase({
                id: 'm-touch-1933',
                issue: 'MOBMARKET-7715',
                test() {
                    return this.browser.allure.runStep('Проверяем открытие каталога по клику на "все категории"', () =>
                        this.bubbleCategories.clickActionBubble()
                            .then(() => this.menuCatalog.isVisible())
                            .should.eventually.to.be.equal(true, 'Каталог открылся')
                    );
                },
            }),
        },
        'По клику на кнопку назад': {
            'закрывается каталог': makeCase({
                id: 'm-touch-3176',
                issue: 'MARKETFRONT-6787',
                test() {
                    return this.browser.allure.runStep('Проверяем каталога по клику на ккнопку назад', () =>
                        this.bubbleCategories.clickActionBubble()
                            .then(() => this.menuCatalog.isVisible())
                            .should.eventually.to.be.equal(true, 'Каталог открылся')
                            .then(() => this.browser.back())
                            .then(() => this.menuCatalog.isVisible())
                            .should.eventually.to.be.equal(false, 'Каталог закрылся')
                    );
                },
            }),
            'переходит к предыдущей категории': makeCase({
                id: 'm-touch-3176',
                issue: 'MARKETFRONT-6787',
                async test() {
                    await this.bubbleCategories.clickActionBubble();

                    const categoryText = await this.menuCatalog.getCategoryTitle(1);

                    this.menuCatalog.clickCategoryByIndex(1);
                    await this.allure.runStep(
                        'Дожидаемся отрисовки категории в каталоге',
                        this.browser.waitUntil(
                            async () => {
                                const headerText = await this.menuCatalog.getHeaderTitle();
                                return (headerText !== 'Каталог' && headerText !== 'Все товары');
                            }, 1500
                        )
                    );

                    let headerText = await this.menuCatalog.getHeaderTitle();

                    await this.expect(headerText).to.equal(categoryText, 'Проверяем совпадение заголовков');
                    this.browser.back();
                    headerText = await this.menuCatalog.getHeaderTitle();
                    await this.expect(headerText).to.equal('Каталог', 'Проверяем совпадение заголовков');
                },
            }),
        },
        'По клику на первую категорию в каталоге': {
            'открывается соответствующая категория в каталоге': makeCase({
                id: 'm-touch-1943',
                issue: 'MOBMARKET-7749',
                async test() {
                    await this.bubbleCategories.clickActionBubble();

                    const categoryText = await this.menuCatalog.getCategoryTitle(1);

                    this.menuCatalog.clickCategoryByIndex(1);
                    await this.allure.runStep(
                        'Дожидаемся отрисовки категории в каталоге',
                        this.browser.waitUntil(
                            async () => {
                                const headerText = await this.menuCatalog.getHeaderTitle();
                                return (headerText !== 'Каталог' && headerText !== 'Все товары');
                            }, 1500
                        )
                    );

                    const headerText = await this.menuCatalog.getHeaderTitle();

                    return this.expect(headerText).to.equal(categoryText, 'Проверяем совпадение заголовков');
                },
            }),
        },

        'По клику на категорию виджета': {
            'происходит редирект в эту категорию': makeCase({
                id: 'm-touch-1931',
                issue: 'MOBMARKET-7714',
                test() {
                    return this.browser.allure.runStep('Проверяем ссылку navnode навигации', () =>
                        this.bubbleCategories.getCategoryLink()
                            .should.eventually.be.link({
                                pathname: 'catalog--[\\w-]+/\\d+',
                            }, {
                                mode: 'match',
                                skipProtocol: true,
                                skipHostname: true,
                            }))
                        .then(() =>
                            this.browser.yaWaitForPageReloaded(() =>
                                this.bubbleCategories.clickNavnodeByIndex(1)
                            )
                        )
                        .then(() => this.browser.allure.runStep('Проверяем URL страницы после перехода', () =>
                            this.browser
                                .getUrl()
                                .should.eventually.be.link({
                                    pathname: 'catalog--[\\w-]+/\\d+',
                                }, {
                                    mode: 'match',
                                    skipProtocol: true,
                                    skipHostname: true,
                                }))
                        );
                },
            }),
        },
    },
});
