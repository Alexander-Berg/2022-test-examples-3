import {makeSuite} from 'ginny';

import {prepareMultiCartState}
    from '@self/root/src/spec/hermione/scenarios/cartResource';
import {openCartPage}
    from '@self/root/src/spec/hermione/scenarios/cart';
import {prepareUserLastState}
    from '@self/root/src/spec/hermione/scenarios/persAddressResource';


import GroupedParcel
    from '@self/root/src/widgets/content/checkout/common/CheckoutParcel/components/GroupedParcel/__pageObject';
import EditableCard from '@self/root/src/components/EditableCard/__pageObject';
import AddressCard from '@self/root/src/components/AddressCard/__pageObject';
import LoginAgitation
    from '@self/root/src/widgets/content/cart/CartCheckoutControl/components/LoginAgitation/__pageObject';
import PopupBase from '@self/root/src/components/PopupBase/__pageObject';
import Clickable from '@self/root/src/components/Clickable/__pageObject';
/**
 * @ifLose заменить на старые импорты из .../components/DeliveryIntervals/__pageObject
 * @expFlag all_checkout_new_intervals [обратный эксперимент]
 * @ticket MARKETFRONT-58113
 * @start
 */
import {
    DateSelect,
    // eslint-disable-next-line max-len
} from '@self/root/src/widgets/content/checkout/common/CheckoutParcel/components/DateIntervalSelector/__pageObject';

import {
    TimeSelect,
    // eslint-disable-next-line max-len
} from '@self/root/src/widgets/content/checkout/common/CheckoutParcel/components/TimeIntervalSelector/__pageObject';
/**
 * @expFlag all_checkout_new_intervals [обратный эксперимент]
 * @ticket MARKETFRONT-58113
 * @end
 */
import {SelectPopover}
    from '@self/root/src/components/Select/__pageObject';

import cartAndCheckout from './cartAndCheckout';
import {carts} from './mocks';

export default makeSuite('Сохранение стейта при переключении между страницами.',
    {
        issue: 'MARKETFRONT-50231',
        environment: 'kadavr',
        story: {
            async beforeEach() {
                this.setPageObjects({
                    groupedParcel: () => this.createPageObject(GroupedParcel),
                    editGroupedParcelCard: () => this.createPageObject(
                        EditableCard,
                        {parent: this.groupedParcel}
                    ),
                    addressCard: () => this.createPageObject(AddressCard, {
                        parent: this.editGroupedParcelCard,
                    }),
                    dateSelect: () => this.createPageObject(DateSelect),
                    timeSelect: () => this.createPageObject(TimeSelect),
                    selectPopover: () => this.createPageObject(SelectPopover),
                    loginAgitationWrapper: () => this.createPageObject(LoginAgitation),
                    agitationModal: () => this.createPageObject(PopupBase, {
                        parent: this.loginAgitationWrapper,
                    }),
                    notNowButton: () => this.createPageObject(Clickable, {
                        parent: this.agitationModal,
                        root: '[data-autotest-id="notNow"]',
                    }),
                });

                await this.browser.yaScenario(this, prepareUserLastState);

                await this.browser.yaScenario(
                    this,
                    prepareMultiCartState,
                    carts
                );

                await this.browser.yaScenario(this, openCartPage);
            },
            'Сохранение стейта при переключении между страницами корзины и чекаута.': cartAndCheckout,
        },
    }
);
