import {makeCase, makeSuite} from 'ginny';

import HeaderTabs from '@self/platform/widgets/content/HeaderTabs/__pageObject';
import MenuTab from '@self/platform/widgets/content/HeaderTabs/MenuTab/__pageObject';

import {EXPRESS_ROOT_NAVNODE_ID} from '@self/root/src/constants/express';

export const expressEntryointsSuite = makeSuite('Точки входа в экспресс.', {
    feature: 'Точки входа в экспресс',
    environment: 'kadavr',
    issue: 'MARKETFRONT-56168',
    id: 'marketfront-5090',
    story: {
        async beforeEach() {
            this.setPageObjects({
                headerTabs: () => this.createPageObject(HeaderTabs),
                menuTab: () => this.createPageObject(MenuTab, {parent: this.headerTabs.getTabContainerByIndex(0)}),
            });
        },
        'Точка входа в корневой раздел экспресса первая в меню.': makeCase({
            async test() {
                await this.browser.allure.runStep(
                    'Получаем текст ссылки на раздел "Экспресс"', async () => {
                        await this.menuTab.getLinkText().should.eventually.to.be.equal('Экспресс', 'текст ссылки должен быть "Экспресс"');
                    }
                );

                await this.browser.allure.runStep(
                    'Получаем адресс ссылки на раздел "Экспресс"',
                    async () => {
                        await this.menuTab.getLinkHref().should.eventually.to.be.link({
                            pathname: `/catalog--express/${EXPRESS_ROOT_NAVNODE_ID}`,
                        }, {
                            mode: 'match',
                            skipProtocol: true,
                            skipHostname: true,
                            skipQuery: true,
                        });
                    });
            },
        }),
    },
});
