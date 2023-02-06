import {makeSuite, mergeSuites, prepareSuite} from 'ginny';

// suites
import DeliveryPickupTextSuite from '@self/platform/spec/hermione/test-suites/blocks/n-delivery/pickupText';
// page-objects
import Delivery from '@self/platform/spec/page-objects/components/DeliveryInfo';
import SnippetList from '@self/platform/widgets/content/productOffers/Results/__pageObject';
// mocks
import {ROUTE} from '@self/platform/spec/hermione/fixtures/product/common';
import productWithPickup from '@self/platform/spec/hermione/fixtures/pickup/free_today_manyPoints';

export default makeSuite('Условия самовывоза на офферной выдаче карточки модели', {
    environment: 'kadavr',
    story: {
        'Оффер. Бесплатно сегодня.': mergeSuites({
            async beforeEach() {
                await this.browser.setState(
                    'report',
                    productWithPickup.state
                );

                await this.browser.yaOpenPage(
                    'market:product-offers',
                    ROUTE
                );
            },
        },
        prepareSuite(DeliveryPickupTextSuite, {
            meta: {
                id: 'marketfront-3795',
                issue: 'MARKETFRONT-5889',
            },
            pageObjects: {
                productSnippetList() {
                    return this.createPageObject(SnippetList);
                },
                delivery() {
                    return this.createPageObject(Delivery, {
                        parent: this.productSnippetList,
                    });
                },
            },
            params: {
                expectedText: 'Самовывоз сегодня — бесплатно',
            },
        })

        /*
        * Точка входа удалена. Задача на возврат тестов https://st.yandex-team.ru/MARKETFRONT-71298
        prepareSuite(DeliveryPickupMapLinkSuite, {
            meta: {
                id: 'marketfront-3796',
                issue: 'MARKETFRONT-5894',
            },
            pageObjects: {
                productSnippetList() {
                    return this.createPageObject(SnippetList);
                },
                delivery() {
                    return this.createPageObject(Delivery, {
                        parent: this.productSnippetList,
                    });
                },
            },
            params: {
                slug: ROUTE.slug,
                productId: ROUTE.productId,
                fesh: BUSINESS_ID,
            },
        })*/
        ),
    },
});
