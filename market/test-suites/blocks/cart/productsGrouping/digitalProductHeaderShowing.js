import {makeCase, makeSuite} from 'ginny';
import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';
import {prepareMultiCartState} from '@self/root/src/spec/hermione/scenarios/cartResource';
import {buildCheckouterBucket} from '@self/root/src/spec/utils/checkouter';
import {waitForCartActualization} from '@self/root/src/spec/hermione/scenarios/cart';

import * as digital from '@self/root/src/spec/hermione/kadavr-mock/report/digital';

import ParcelTitle from '@self/root/src/widgets/content/cart/CartList/components/ParcelTitle/__pageObject';
import BusinessGroupsStrategiesSelector
    from '@self/root/src/widgets/content/cart/CartList/components/BusinessGroupsStrategiesSelector/__pageObject';

const digitalCart = buildCheckouterBucket({
    cartIndex: 0,
    items: [{
        skuMock: digital.skuMock,
        offerMock: digital.offerMock,
        count: 1,
    }],
    isDigital: true,
});

module.exports = makeSuite('Отображение подзаголовка в группе цифровых товаров.', {
    environment: 'kadavr',
    story: {
        async beforeEach() {
            this.setPageObjects({
                title: () => this.createPageObject(
                    ParcelTitle,
                    {
                        root: `${BusinessGroupsStrategiesSelector.bucket(0)} ${ParcelTitle.root}`,
                    }
                ),
            });

            await this.browser.yaScenario(
                this,
                prepareMultiCartState,
                [digitalCart]
            );

            await this.browser.yaOpenPage(PAGE_IDS_COMMON.CART, {lr: 213});
            return this.browser.yaScenario(this, waitForCartActualization);
        },

        'На странице отображаются товар в группе "Получение по электронной почте".': makeCase({
            async test() {
                return this.title.getTitleText().should.be.eventually.equal('Получение по электронной почте');
            },
        }),

        'В группе отображается подзаголовок "Вам придут код активации и инструкция".': makeCase({
            async test() {
                return this.title.getSubtitleText().should.be.eventually.equal('Вам придут код активации и инструкция');
            },
        }),
    },
});
