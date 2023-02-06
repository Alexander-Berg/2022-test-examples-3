import {makeSuite, makeCase} from '@yandex-market/ginny';

// page-objects
import ExpressCatalogEntrypointsPO from '@self/root/src/widgets/content/ExpressCatalogEntrypoints/__pageObject';

/**
 * @param {PageObject.ExpressCatalogEntrypoints} expressCatalogEntrypoints
 */
export default makeSuite('Виджет точек входа на экспресс-категории в Yandex Go.', {
    environment: 'kadavr',
    tags: ['Контур#Интеграции'],
    id: 'm-touch-3855',
    story: {
        async beforeEach() {
            hermione.setPageObjects.call(this, {
                expressCatalogEntrypoints: () => this.browser.createPageObject(ExpressCatalogEntrypointsPO),
            });
        },
        'По-умолчанию': {
            'отображается': makeCase({
                async test() {
                    await this.expressCatalogEntrypoints.waitForVisible();

                    return this.expressCatalogEntrypoints.isVisible()
                        .should.eventually.be.equal(true, 'Точки входа на категории отображаются');
                },
            }),
        },
        'Плиточки категорий': {
            'по-умолчанию': {
                'содержат ссылки на категорийную выдачу': makeCase({
                    async test() {
                        await this.expressCatalogEntrypoints.waitForVisible();

                        const entrypointsLinks = await this.expressCatalogEntrypoints.getEntrypointsUrls();

                        return Promise.all(
                            entrypointsLinks.map(link => this.browser.expect(link).to.be.link({
                                pathname: '/yandex-go/search',
                            }, {
                                mode: 'match',
                                skipProtocol: true,
                                skipHostname: true,
                                skipQuery: true,
                            }))
                        );
                    },
                }),
            },
        },
    },
});
