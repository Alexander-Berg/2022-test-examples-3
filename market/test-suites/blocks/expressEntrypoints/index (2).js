import {makeCase, makeSuite} from 'ginny';

import RowListCategories from '@self/platform/widgets/parts/RowListCategories/__pageObject';
import {EXPRESS_ROOT_NAVNODE_ID} from '@self/root/src/constants/express';

export const expressEntryointsSuite = makeSuite('Точки входа в экспресс.', {
    feature: 'Точки входа в экспресс',
    environment: 'kadavr',
    issue: 'MARKETFRONT-56168',
    id: 'marketfront-5090',
    story: {
        async beforeEach() {
            this.setPageObjects({
                categoriesList: () => this.createPageObject(RowListCategories),
            });
        },
        'Точка входа в корневой раздел экспресса первая в меню.': makeCase({
            async test() {
                await this.browser.allure.runStep(
                    'Получаем текст первой ссылки', async () => {
                        await this.categoriesList.getNavnodeByIndex(0).getText().should.eventually.to.be.equal('Экспресс', 'текст ссылки должен быть "Экспресс"');
                    }
                );

                await this.browser.allure.runStep(
                    'Получаем адресс первой ссылки',
                    async () => {
                        await this.categoriesList.getNavnodeByIndex(0).getAttribute('href').should.eventually.to.be.link({
                            pathname: `/catalog--express/${EXPRESS_ROOT_NAVNODE_ID}`,
                        }, {
                            mode: 'match',
                            skipProtocol: true,
                            skipHostname: true,
                            skipQuery: true,
                        }, 'ссылка должна вести на страницу экспресса');
                    });
            },
        }),
    },
});
