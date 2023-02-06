import {makeSuite, makeCase, prepareSuite, mergeSuites} from 'ginny';

import ConditionTypeSuite from '@self/platform/spec/hermione/test-suites/blocks/ConditionType';
import ConditionType from '@self/project/src/components/ConditionType/__pageObject';

/**
 * @param {this.params.expectedConditionType}
 * @param {this.params.expectedReason}
 * @property {PageObject.OfferCondition} this.offerCondition
 */
export default makeSuite('Информация об уценённом товаре', {
    params: {
        expectedConditionType: 'Ожидаемое состояние товара',
        expectedReason: 'Ожидаемая причина уценки товара',
    },
    feature: 'б/у товары',
    story: mergeSuites(
        prepareSuite(ConditionTypeSuite, {
            pageObjects: {
                conditionType() {
                    return this.createPageObject(ConditionType);
                },
            },
        }),
        {
            'По умолчанию': {
                'отображается': makeCase({
                    async test() {
                        const isExisting = this.offerCondition.isExisting();

                        return this.expect(isExisting).to.be.equal(true, 'Информация о состоянии товара');
                    },
                }),
            },
            'Причина уценки': {
                'по умолчанию': {
                    'присутствует в блоке': makeCase({
                        async test() {
                            const reason = await this.offerCondition.getReasonValue();
                            return this.expect(reason).to.be.equal(
                                this.params.expectedReason,
                                `Причина уценки товара должна быть "${this.params.expectedReason}"`
                            );
                        },
                    }),
                },
            },
        }
    ),
});
