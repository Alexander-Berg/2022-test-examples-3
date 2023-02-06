import {makeSuite} from 'ginny';

import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';
import checkoutItemIds from '@self/root/src/spec/hermione/configs/checkout/items';
import {prepareOrder} from '@self/root/src/spec/hermione/scenarios/checkoutResource';
import returnsFormData from '@self/root/src/spec/hermione/configs/returns/formData';
import {setReportState} from '@self/root/src/spec/hermione/scenarios/kadavr';
import {createShopInfo, mergeState} from '@yandex-market/kadavr/mocks/Report/helpers';
import {
    fillReturnFormAndGoToMapStep,
    prepareReturnStateAndOpenReturnPage,
} from '@self/root/src/spec/hermione/scenarios/returns';
import {ReturnItemReason} from '@self/root/src/widgets/parts/ReturnCandidate/components/ReturnItemReason/__pageObject';
import {Final} from '@self/root/src/widgets/parts/ReturnCandidate/components/Final/__pageObject';
import RecipientForm from '@self/root/src/components/RecipientForm/__pageObject';
import {ReturnItems} from '@self/root/src/widgets/parts/ReturnCandidate/components/ReturnItems/__pageObject';
import {Credentials} from '@self/root/src/widgets/parts/ReturnCandidate/components/Credentials/__pageObject';
import ReturnMapOutletInfo from '@self/root/src/widgets/parts/ReturnCandidate/widgets/ReturnMapOutletInfo/__pageObject';
import {ORDER_STATUS} from '@self/root/src/entities/order';
import {DELIVERY_PARTNERS, DELIVERY_TYPES} from '@self/root/src/constants/delivery';
import {
    SHOP_ID,
    SHOP_NAME,
    SHOP_RETURN_CONTACTS,
} from '@self/root/src/spec/hermione/kadavr-mock/returns/shopReturnContacts';
import PlacemarkMap from '@self/root/src/components/PlacemarkMap/__pageObject';
import {yandexMarketPickupPoint} from '@self/root/src/spec/hermione/kadavr-mock/returns/reportMoscowReturnOutlets';
import {cloneOutlet, moveOutlets} from '@self/root/src/spec/utils/outlet';
import {selectOutletAndGoToNextStep} from '@self/root/src/spec/hermione/scenarios/returns/common';
import {Account} from '@self/root/src/widgets/parts/ReturnCandidate/components/Account/__pageObject';
import {Reason} from '@self/root/src/widgets/parts/ReturnCandidate/components/Reason/__pageObject';

export default makeSuite('Возврат FBS-заказов в брендированные пункты выдачи заказов Яндекс.Маркет', {
    environment: 'kadavr',
    issue: 'MARKETFRONT-48914',
    params: {
        items: 'Товары',
    },
    defaultParams: {
        items: [{
            skuId: checkoutItemIds.asus.skuId,
            offerId: checkoutItemIds.asus.offerId,
            wareMd5: checkoutItemIds.asus.offerId,
            count: 2,
            id: 11111,
        }],
        returnContacts: [
            SHOP_RETURN_CONTACTS.PERSON,
            SHOP_RETURN_CONTACTS.POST,
            SHOP_RETURN_CONTACTS.CARRIER,
            SHOP_RETURN_CONTACTS.SELF,
        ],
    },
    feature: 'Возврат FBS-заказов в брендированные пункты выдачи заказов Яндекс.Маркет',
    story: {
        async beforeEach() {
            this.setPageObjects({
                reasonTypeSelector: () => this.createPageObject(ReturnItemReason, {parent: this.returnsForm}),
                returnItemsScreen: () => this.createPageObject(ReturnItems, {parent: this.returnsForm}),
                buyerInfoScreen: () => this.createPageObject(Credentials, {parent: this.returnsForm}),
                recipientForm: () => this.createPageObject(RecipientForm, {parent: this.returnsForm}),
                finalScreen: () => this.createPageObject(Final, {parent: this.returnsForm}),
                returnMap: () => this.createPageObject(PlacemarkMap, {parent: this.returnsForm}),
                returnMapOutletInfo: () => this.createPageObject(ReturnMapOutletInfo, {parent: this.returnsForm}),
                reasonsChooseScreen: () => this.createPageObject(Reason, {parent: this.returnsForm}),
                account: () => this.createPageObject(Account, {parent: this.returnsForm}),
            });

            await this.browser.setState(
                'Checkouter.returnableItems',
                this.params.items.map(item => ({
                    ...item,
                    itemId: item.id,
                }))
            );

            await this.browser.setState('schema', {
                mdsPictures: [{
                    groupId: 3723,
                    imageName: '2a000001654282aec0648192ce44a1708325',
                }],
            });

            const shopInfo = createShopInfo({
                returnDeliveryAddress: 'hello, there!',
                shopName: SHOP_NAME,
            }, SHOP_ID);

            await this.browser.setState('ShopInfo', {
                returnContacts: this.params.returnContacts,
            });

            await this.browser.yaScenario(this, setReportState, {
                state: mergeState([shopInfo]),
            });

            const result = await this.browser.yaScenario(this, prepareOrder, {
                region: this.params.region,
                orders: [{
                    items: this.params.items,
                    deliveryType: DELIVERY_TYPES.DELIVERY,
                    delivery: {
                        deliveryPartnerType: DELIVERY_PARTNERS.YANDEX_MARKET,
                    },
                    shopId: SHOP_ID,
                }],
                paymentType: 'POSTPAID',
                paymentMethod: 'YANDEX',
                status: ORDER_STATUS.DELIVERED,
                fulfilment: false,
            }, {
                bankDetails: returnsFormData.bankAccount,
            });

            const orderId = result.orders[0].id;
            this.params.orderId = orderId;

            await this.browser.yaProfile('pan-topinambur', PAGE_IDS_COMMON.CREATE_RETURN, {orderId});
        },
        'Регион Москва с брендированными пунктами выдачи заказов Яндекс.Маркет': {
            id: 'marketfront-5094',
            async test() {
                const MOSCOW_CENTER_PLACE_COORDINATES = {
                    longitude: 37.622735,
                    latitude: 55.753995,
                };

                /**
                 * @see {@link selectIsOutletCountTooSmallToShowMap}
                 */
                const OUTLETS_AMOUNT_TO_SHOW_MAP = 50;

                const marketBrandedPickupPoints = cloneOutlet(yandexMarketPickupPoint, OUTLETS_AMOUNT_TO_SHOW_MAP);

                moveOutlets(marketBrandedPickupPoints, MOSCOW_CENTER_PLACE_COORDINATES, {distribute: true});

                const deliveryOption = {
                    type: 'PICKUP',
                    deliveryServiceId: 1003563,
                    outletIds: marketBrandedPickupPoints.map(({id}) => id),
                    marketPartner: true,
                    price: {
                        price: 0,
                        currency: 'RUR',
                    },
                };

                await this.browser.yaScenario(this, prepareReturnStateAndOpenReturnPage, {
                    items: this.params.items,
                    returnOptionsMock: {
                        deliveryOptions: [deliveryOption],
                        items: [
                            {
                                id: 9412299,
                                itemId: 9412299,
                                isDeliveryService: false,
                                count: 1,
                                reasonType: 'DO_NOT_FIT',
                                picturesUrls: [],
                            },
                        ],
                        largeSize: false,
                    },
                    outletsMock: marketBrandedPickupPoints,
                });

                const [firstMarketBrandedPickupPoint] = marketBrandedPickupPoints;

                await this.browser.yaScenario(this, fillReturnFormAndGoToMapStep);

                await this.returnMap.waitForReady(3000);

                await this.browser.yaScenario(
                    this,
                    selectOutletAndGoToNextStep,
                    {outlet: firstMarketBrandedPickupPoint}
                );

                await this.account.submit();

                await this.finalScreen.marketPickupPointSteps.isVisible();
            },
        },
    },
});
