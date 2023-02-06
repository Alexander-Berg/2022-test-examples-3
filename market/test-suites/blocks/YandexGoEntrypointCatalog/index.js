import {makeSuite, makeCase} from 'ginny';

/**
 * @param {PageObject.YandexGoCatalog} yandexGoCatalog
 * @param {number} params.nid - идентификатор категории
 * @param {number} params.regionId - идентификатор региона
 * @param {string} params.gpsCoordinate - gps координата
 */

export default makeSuite('Каталог экспресс товаров Yandex Go в виде меню навигации.', {
    environment: 'kadavr',
    tags: ['Контур#Интеграции'],
    story: {
        'Строка категории': {
            'по умолчанию': {
                'содержит правильный URL': makeCase({
                    issue: 'MARKETFRONT-59080',
                    id: 'm-touch-3782',
                    async test() {
                        const {nid, regionId, gpsCoordinate} = this.params;
                        await this.yandexGoCatalog.waitForVisible();
                        const url = await this.yandexGoCatalog.getNthCategoryLinkUrl(1);

                        return this.expect(url).to.be.link({
                            pathname: '/yandex-go/search',
                            query: {
                                nid,
                                lr: regionId,
                                gps: gpsCoordinate,
                            },
                        }, {
                            mode: 'match',
                            skipProtocol: true,
                            skipHostname: true,
                        });
                    },
                }),
            },
            'при клике': {
                'выполняет переход на другую страницу': makeCase({
                    issue: 'MARKETFRONT-59080',
                    id: 'm-touch-3783',
                    async test() {
                        await this.yandexGoCatalog.waitForVisible();

                        return this.browser.allure.runStep(
                            'Клик по категории уводит пользователя с текущей страницы',
                            () => this.browser.yaWaitForPageUnloaded(
                                () => this.yandexGoCatalog.clickNthCategory(1)
                            )
                        );
                    },
                }),
            },
        },
    },
});
