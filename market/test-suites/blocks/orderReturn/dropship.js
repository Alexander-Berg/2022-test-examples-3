import {
    makeSuite,
    makeCase,
} from 'ginny';
import {replaceBreakChars} from '@self/root/src/spec/utils/text';
import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';

import checkoutItemIds from '@self/root/src/spec/hermione/configs/checkout/items';
import {prepareOrder} from '@self/root/src/spec/hermione/scenarios/checkoutResource';
import returnsFormData from '@self/root/src/spec/hermione/configs/returns/formData';
import {setReportState} from '@self/root/src/spec/hermione/scenarios/kadavr';
import {mergeState, createShopInfo} from '@yandex-market/kadavr/mocks/Report/helpers';
import {createReturnPositiveScenario} from '@self/root/src/spec/hermione/scenarios/returns';

import ReturnsPage from '@self/root/src/widgets/parts/ReturnCandidate/components/View/__pageObject';
import ReturnContactItem from '@self/root/src/widgets/parts/ReturnCandidateContacts/components/ReturnContactItem/__pageObject';
import Modal from '@self/root/src/components/PopupBase/__pageObject';
import Phone from '@self/root/src/components/Phone/__pageObject';
import {ReturnItemReason} from '@self/root/src/widgets/parts/ReturnCandidate/components/ReturnItemReason/__pageObject';
import {Account} from '@self/root/src/widgets/parts/ReturnCandidate/components/Account/__pageObject';
import {Submit} from '@self/root/src/widgets/parts/ReturnCandidate/components/Submit/__pageObject';
import {Final} from '@self/root/src/widgets/parts/ReturnCandidate/components/Final/__pageObject';
import DefaultText from '@self/root/src/widgets/parts/ReturnCandidate/components/Final/components/DefaultText/__pageObject';
import DropshipDefaultText from '@self/root/src/widgets/parts/ReturnCandidate/components/Final/components/DropshipDefaultText/__pageObject';
import {BankAccountForm} from '@self/root/src/components/BankAccountForm/__pageObject';
import RecipientForm from '@self/root/src/components/RecipientForm/__pageObject';
import {ReturnItems} from '@self/root/src/widgets/parts/ReturnCandidate/components/ReturnItems/__pageObject';
import {Credentials} from '@self/root/src/widgets/parts/ReturnCandidate/components/Credentials/__pageObject';

import {ORDER_STATUS} from '@self/root/src/entities/order';
import {DELIVERY_TYPES, DELIVERY_PARTNERS} from '@self/root/src/constants/delivery';
import RETURN_TEXT_CONSTANTS from '@self/root/src/widgets/parts/ReturnCandidate/constants/i18n';
import {
    SHOP_ID,
    SHOP_NAME,
    SHOP_RETURN_CONTACTS,
} from '@self/root/src/spec/hermione/kadavr-mock/returns/shopReturnContacts';

const ID = 11111;

const FIRST_STEP_TITLE = replaceBreakChars(
    RETURN_TEXT_CONSTANTS.FINAL_FIRST_STEP_TITLE
);

const SECOND_STEP_TITLE = replaceBreakChars(
    `${RETURN_TEXT_CONSTANTS.FINAL_DROPSHIP_SECOND_STEP_TITLE}${SHOP_NAME}`
);

export default makeSuite('Дропшип.', {
    environment: 'kadavr',
    issue: 'BLUEMARKET-12369',
    id: 'BLUEMARKET-12369',
    params: {
        items: 'Товары',
    },
    defaultParams: {
        items: [{
            skuId: checkoutItemIds.asus.skuId,
            offerId: checkoutItemIds.asus.offerId,
            wareMd5: checkoutItemIds.asus.offerId,
            count: 1,
            id: ID,
        }],
        returnContacts: [
            SHOP_RETURN_CONTACTS.PERSON,
            SHOP_RETURN_CONTACTS.POST,
            SHOP_RETURN_CONTACTS.CARRIER,
            SHOP_RETURN_CONTACTS.SELF,
        ],
    },
    feature: 'Дропшип.',
    story: {
        async beforeEach() {
            this.setPageObjects({
                returnsForm: () => this.createPageObject(ReturnsPage),
                reasonTypeSelector: () =>
                    this.createPageObject(ReturnItemReason, {parent: this.returnsForm}),
                returnItemsScreen: () => this.createPageObject(
                    ReturnItems,
                    {parent: this.returnsForm}
                ),
                buyerInfoScreen: () => this.createPageObject(Credentials, {parent: this.returnsForm}),
                returnsMoney: () => this.createPageObject(Account, {parent: this.returnsForm}),
                bankAccountForm: () => this.createPageObject(BankAccountForm, {parent: this.returnsMoney}),
                recipientForm: () => this.createPageObject(RecipientForm, {parent: this.returnsForm}),
                submitForm: () => this.createPageObject(Submit, {parent: this.returnsForm}),
                finalScreen: () => this.createPageObject(Final, {parent: this.returnsForm}),
                returnsFinalDefaultText: () => this.createPageObject(DefaultText, {parent: this.finalScreen}),
                returnsFinalDropshipDefaultText: () => this.createPageObject(DropshipDefaultText, {parent: this.finalScreen}),
                supportPhone: () => this.createPageObject(Phone, {parent: this.returnsFinalDropshipDefaultText}),
                returnContactItemPost: () => this.createPageObject(ReturnContactItem, {
                    parent: this.returnsFinalDropshipDefaultText,
                    root: `${ReturnContactItem.root}[data-auto="POST"]`,
                }),
                returnContactItemCarrier: () => this.createPageObject(ReturnContactItem, {
                    parent: this.returnsFinalDropshipDefaultText,
                    root: `${ReturnContactItem.root}[data-auto="CARRIER"]`,
                }),
                returnContactItemSelf: () => this.createPageObject(ReturnContactItem, {
                    parent: this.returnsFinalDropshipDefaultText,
                    root: `${ReturnContactItem.root}[data-auto="SELF"]`,
                }),
                modal: () => this.createPageObject(Modal),
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

        'Экран успешного оформления заявления.': {
            async beforeEach() {
                await this.browser.yaScenario(this, createReturnPositiveScenario, {
                    shouldMapStepBeShown: false,
                });

                await this.finalScreen.waitForRootIsVisible()
                    .should.eventually.to.be.equal(true, 'Экран успешного оформления заявления должен отобразиться');

                await this.returnsFinalDropshipDefaultText.isVisible()
                    .should.eventually.to.be.equal(true, 'Экран успешного оформления заявления для дропшипа должен отобразиться');
            },

            'Контакты для возврата.': {
                'Есть контакты для возврата.': {
                    'Тексты': makeCase({
                        async test() {
                            await this.returnsFinalDropshipDefaultText.getStepTitleTextByIndex(1)
                                .should.eventually.to.be.equal(
                                    FIRST_STEP_TITLE,
                                    `Заголовок первого шага "${FIRST_STEP_TITLE}"`
                                );

                            await this.returnsFinalDropshipDefaultText.getStepTitleTextByIndex(2)
                                .should.eventually.to.have.string(
                                    SECOND_STEP_TITLE,
                                    `Заголовок второго шага "${SECOND_STEP_TITLE}"`
                                );
                        },
                    }),

                    'Доставка почтой': makeCase({
                        async test() {
                            await this.returnsFinalDropshipDefaultText.getStepTitleTextByIndex(2)
                                .should.eventually.to.have.string(
                                    SECOND_STEP_TITLE,
                                    `Заголовок второго шага "${SECOND_STEP_TITLE}"`
                                );

                            await this.returnContactItemPost.isVisible()
                                .should.eventually.to.be.equal(
                                    false,
                                    'Доставка почтой не должна быть видна'
                                );
                        },
                    }),

                    'Доставка курьерской службой.': {
                        'Контакты для возврата правильные.': makeCase({
                            async test() {
                                await this.returnsFinalDropshipDefaultText.getStepTitleTextByIndex(2)
                                    .should.eventually.to.have.string(
                                        SECOND_STEP_TITLE,
                                        `Заголовок второго шага "${SECOND_STEP_TITLE}"`
                                    );

                                await this.returnContactItemCarrier.isVisible()
                                    .should.eventually.to.be.equal(
                                        true,
                                        'Доставка курьерской службой должна быть видна'
                                    );

                                await this.returnContactItemCarrier.getAddressText()
                                    .should.eventually.to.be.equal(
                                        SHOP_RETURN_CONTACTS.CARRIER.address,
                                        `Адрес доставки курьерской службой должен быть ${SHOP_RETURN_CONTACTS.CARRIER.address}`
                                    );

                                const name = `${SHOP_RETURN_CONTACTS.CARRIER.lastName} ${SHOP_RETURN_CONTACTS.CARRIER.firstName} ` +
                                    SHOP_RETURN_CONTACTS.CARRIER.middleName;
                                await this.returnContactItemCarrier.getNameText()
                                    .should.eventually.to.be.equal(
                                        name,
                                        `Имя получателя доставки курьерской службой должно быть ${name}`
                                    );

                                await this.returnContactItemCarrier.getJobPositionText()
                                    .should.eventually.to.be.equal(
                                        SHOP_RETURN_CONTACTS.CARRIER.jobPosition,
                                        `Должность получатель доставки курьерской службой должна быть ${SHOP_RETURN_CONTACTS.CARRIER.jobPosition}`
                                    );

                                await this.returnContactItemCarrier.getPhoneNumberText()
                                    .should.eventually.to.be.equal(
                                        SHOP_RETURN_CONTACTS.CARRIER.phoneNumber,
                                        `Телефон получателя доставки курьерской службой должен быть ${SHOP_RETURN_CONTACTS.CARRIER.phoneNumber}`
                                    );
                            },
                        }),
                    },

                    'Доставка пользователем лично': makeCase({
                        async test() {
                            await this.returnsFinalDropshipDefaultText.getStepTitleTextByIndex(2)
                                .should.eventually.to.have.string(
                                    SECOND_STEP_TITLE,
                                    `Заголовок второго шага "${SECOND_STEP_TITLE}"`
                                );

                            await this.returnContactItemSelf.isVisible()
                                .should.eventually.to.be.equal(
                                    true,
                                    'Доставка пользователем лично должна быть видна'
                                );

                            await this.returnContactItemSelf.getAddressText()
                                .should.eventually.to.be.equal(
                                    SHOP_RETURN_CONTACTS.SELF.address,
                                    `Адрес доставки должен быть ${SHOP_RETURN_CONTACTS.SELF.address}`
                                );

                            await this.returnContactItemSelf.getCommentsText()
                                .should.eventually.to.be.equal(
                                    SHOP_RETURN_CONTACTS.SELF.comments,
                                    `Комментарий к адресу доставки должен быть ${SHOP_RETURN_CONTACTS.SELF.comments}`
                                );

                            const name = `${SHOP_RETURN_CONTACTS.SELF.lastName} ${SHOP_RETURN_CONTACTS.SELF.firstName} ` +
                                SHOP_RETURN_CONTACTS.SELF.middleName;
                            await this.returnContactItemSelf.getNameText()
                                .should.eventually.to.be.equal(
                                    name,
                                    `Имя получателя доставки должно быть ${name}`
                                );

                            await this.returnContactItemSelf.getJobPositionText()
                                .should.eventually.to.be.equal(
                                    SHOP_RETURN_CONTACTS.SELF.jobPosition,
                                    `Должность получатель доставки должна быть ${SHOP_RETURN_CONTACTS.SELF.jobPosition}`
                                );

                            await this.returnContactItemSelf.getPhoneNumberText()
                                .should.eventually.to.be.equal(
                                    SHOP_RETURN_CONTACTS.SELF.phoneNumber,
                                    `Телефон получателя доставки должен быть ${SHOP_RETURN_CONTACTS.SELF.phoneNumber}`
                                );
                        },
                    }),
                },

                'Нет контактов для возврата': makeCase({
                    defaultParams: {
                        returnContacts: null,
                    },

                    async test() {
                        await this.returnsFinalDropshipDefaultText.getStepTitleTextByIndex(1)
                            .should.eventually.to.be.equal(
                                FIRST_STEP_TITLE,
                                `Заголовок первого шага "${FIRST_STEP_TITLE}"`
                            );

                        await this.returnsFinalDropshipDefaultText.getStepTitleTextByIndex(2)
                            .should.eventually.to.be.equal(
                                RETURN_TEXT_CONSTANTS.FINAL_DROPSHIP_NO_CONTACTS_TITLE,
                                `Заголовок второго шага "${RETURN_TEXT_CONSTANTS.FINAL_DROPSHIP_NO_CONTACTS_TITLE}"`
                            );

                        await this.supportPhone.isVisible()
                            .should.eventually.to.be.equal(
                                false,
                                'Телефон поддержки не должен быть виден'
                            );
                    },
                }),
            },
        },
    },
});
