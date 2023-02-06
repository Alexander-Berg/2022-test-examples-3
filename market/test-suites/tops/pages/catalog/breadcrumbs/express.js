import {mergeSuites, makeSuite, makeCase} from 'ginny';

import BreadcrumbsUnified from '@self/platform/spec/page-objects/BreadcrumbsUnified';
import {mobilePhonesExpress, rootExpress} from '@self/root/src/spec/hermione/kadavr-mock/cataloger/navigationPathExpress';
import {EXPRESS_ROOT_NAVNODE_ID} from '@self/root/src/constants/express';

export const breadcrumbsExpressSuite = makeSuite('Хлебные крошки', {
    environment: 'kadavr',
    issue: 'MARKETFRONT-56168',
    id: 'marketfront-5092',
    story: mergeSuites(
        {
            async beforeEach() {
                await this.browser.setState('Cataloger.tree', mobilePhonesExpress);

                this.setPageObjects({
                    breadcrumbs: () => this.createPageObject(BreadcrumbsUnified),
                });

                return this.browser.yaSimulateBot().yaOpenPage('touch:list', {
                    'nid': 23282330,
                    'filter-express-delivery': 1,
                    'searchContext': 'express',
                }).yaClosePopup(this.regionPopup);
            },
        },
        {
            'Должны содержать точку входа в экспресс': makeCase({
                async test() {
                    await this.browser.allure.runStep(
                        'Получаем адресс ссылки на раздел "Экспресс"',
                        async () => {
                            await this.breadcrumbs.getCrumbLinkHref(1).should.eventually.to.be.link({
                                pathname: `/catalog--express/${EXPRESS_ROOT_NAVNODE_ID}`,
                            }, {
                                mode: 'match',
                                skipProtocol: true,
                                skipHostname: true,
                                skipQuery: true,
                            }, 'Первая крошка должна вести на страницу экспресса');
                        });

                    // Собираемся перейти в корень экспресса, поэтому предварительно устанавливаем правильный мок каталогера
                    await this.browser.setState('Cataloger.tree', rootExpress);

                    await this.browser.allure.runStep('Кликаем на ссылку раздела "Экспресс"',
                        async () => {
                            await this.browser.yaWaitForChangeUrl(() => this.breadcrumbs.getCrumbLinkByIndex(1).click());
                        });

                    // Здесь проверяется только переход в раздел экспресса, а не получение его cms-странички по nid'у
                    await this.browser.getUrl().should.eventually.to.be
                        .link({
                            pathname: `/catalog--express/${EXPRESS_ROOT_NAVNODE_ID}/list`,
                        }, {
                            mode: 'match',
                            skipProtocol: true,
                            skipHostname: true,
                            skipQuery: true,
                        }, 'Должны снова оказаться на странице экспресса');
                },
            }),
        }
    ),
});
