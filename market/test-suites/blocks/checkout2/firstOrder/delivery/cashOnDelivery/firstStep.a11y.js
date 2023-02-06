import {makeCase, makeSuite} from 'ginny';

import DeliveryTypeList from '@self/root/src/components/DeliveryTypes/__pageObject';
import {switchToSpecifiedDeliveryForm} from '@self/root/src/spec/hermione/scenarios/checkout';

const getAriaActivedescendantAttrByCount = (count = 0) =>
    `react-autowhatever-address--item-${count >= 1 ? count - 1 : 0}`;

export default makeSuite('Оформление первого заказа. Шаг 1. Доступность', {
    id: 'marketfront-4425',
    issue: 'MARKETFRONT-45602',
    feature: 'Оформление первого заказа. Шаг 1',
    environment: 'kadavr',
    story: {
        async beforeEach() {
            this.setPageObjects({
                deliveryTypes: () => this.createPageObject(DeliveryTypeList, {
                    parent: this.deliveryEditorCheckoutWizard,
                }),
            });

            await this.deliveryTypes.waitForVisible();
        },
        'Открыть страницу чекаута.': {
            'Отображается экран "Как доставить заказ".': {
                'Должна быть сделана навигация для незрячих': makeCase({
                    async test() {
                        const EXPECTED_TITLE_TEXT = 'Как доставить заказ?';
                        const EXPECTED_TITLE_TAG = 'h1';

                        const title = await this.deliveryEditorCheckoutWizard.title;
                        const titleText = this.deliveryEditorCheckoutWizard.getTitleText();
                        const titleTag = this.browser.getTagName(title.selector);

                        await titleText.should.eventually.to.be.equal(
                            EXPECTED_TITLE_TEXT,
                            `Текст заголовка блока с оформлением заказа должен быть "${EXPECTED_TITLE_TEXT}".`
                        );
                        await titleTag.should.eventually.to.be.equal(
                            EXPECTED_TITLE_TAG,
                            `Тег заголовка блока с оформлением заказа должен быть "${EXPECTED_TITLE_TAG}".`
                        );
                    },
                }),

                'Поля формы должны быть подписаны для незрячих': makeCase({
                    async test() {
                        await this.browser.yaScenario(this, switchToSpecifiedDeliveryForm);
                        const streetFieldSelector = await this.street.getSelector(this.street.input);
                        const streetLabelSelector = await this.street.getSelector(this.street.label);
                        const streetFieldLabelledById = this.browser.getAttribute(streetFieldSelector, 'aria-labelledby');
                        const streetLabelId = await this.browser.getAttribute(streetLabelSelector, 'id');
                        await streetFieldLabelledById.should.eventually.to.be.equal(
                            streetLabelId,
                            'Поле улицы должно быть подписано для незрячих'
                        );

                        const restFields = [
                            'apartament',
                            'floor',
                            'entrance',
                            'intercom',
                            'comment',
                        ];
                        for (let i = 0; i < restFields.length; i++) {
                            const item = restFields[i];

                            const itemField = await this.addressForm[`${item}FormField`];
                            const itemLabel = await this.addressForm[`${item}FormLabel`];
                            const itemFieldLabelledById = this.browser.getAttribute(itemField.selector, 'aria-labelledby');
                            const itemLabelId = await this.browser.getAttribute(itemLabel.selector, 'id');
                            await itemFieldLabelledById.should.eventually.to.be.equal(
                                itemLabelId,
                                `Поле ${item} должно быть подписано для незрячих`
                            );
                        }
                    },
                }),

                'Карта местности должна быть скрыта от незрячих': makeCase({
                    async test() {
                        const map = await this.map.root;
                        const isMapVisuallyHidden = this.browser.getAttribute(map.selector, 'aria-hidden');

                        await isMapVisuallyHidden.should.eventually.to.be.equal(
                            true.toString()
                        );
                    },
                }),

                'Поле ввода адреса доступно с клавиатуры': makeCase({
                    id: 'marketfront-5531',
                    issue: 'MARKETFRONT-60365',
                    async test() {
                        await this.browser.yaScenario(this, switchToSpecifiedDeliveryForm);

                        await this.street.setText('Москва, Льва Толстого 16');
                        const pressDownCount = 2;

                        for (let i = 0; i < pressDownCount; i++) {
                            await this.street.pressKeyDown();
                        }

                        const activedescendant = await this.street.getAriaActivedescendantAttribute();

                        await this.browser.allure.runStep(
                            'id в aria-activedescendant соответствует ожидаемому.',
                            () => {
                                activedescendant.should.to.be.equal(getAriaActivedescendantAttrByCount(pressDownCount));
                            }
                        );
                    },
                }),
            },
        },
    },
});
