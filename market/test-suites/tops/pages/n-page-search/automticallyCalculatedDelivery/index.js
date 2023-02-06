import {makeSuite, prepareSuite} from 'ginny';

// suites
import DeliveryTextSuite from '@self/platform/spec/hermione/test-suites/blocks/n-delivery/__text';
// page-objects
// eslint-disable-next-line max-len
import DeliveryInfo from '@self/project/src/components/Search/Snippet/Offer/common/DeliveryInfo/components/DeliveryInfoContent/__pageObject';
import SnippetCard2 from '@self/project/src/components/Search/Snippet/Card/__pageObject';
import {state} from '@self/platform/spec/hermione/fixtures/delivery/automaticallyCalculated';

export default makeSuite('Автоматический расчёт сроков и стоимости доставки', {
    environment: 'kadavr',
    story: {
        async beforeEach() {
            const routeParams = {
                lr: 213,
                text: 'красный',
                onstock: 1,
            };

            await this.browser.setState(
                'report',
                state
            );

            await this.browser.yaOpenPage('market:search', routeParams);
        },
        'Сниппет.': prepareSuite(DeliveryTextSuite, {
            meta: {
                id: 'marketfront-3365',
                issue: 'MARKETVERSTKA-33870',
            },
            pageObjects: {
                delivery() {
                    return this.createPageObject(DeliveryInfo, {
                        parent: `${SnippetCard2.root}:first-child`,
                    });
                },
            },
            params: {
                expectedText: '≈ 420 ₽ доставка, 3-4 дня',
            },
        }),
    },
});
