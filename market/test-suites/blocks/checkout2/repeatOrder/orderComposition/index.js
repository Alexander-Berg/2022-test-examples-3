import {
    makeSuite,
    prepareSuite,
    mergeSuites,
} from 'ginny';

// pageObjects
import CartItemsDetails
    from '@self/root/src/components/Checkout/CartItemsDetails/__pageObject';
import OrderItemsList from '@self/root/src/components/OrderItemsList/__pageObject';
import ContactCard from '@self/root/src/components/Checkout/ContactCard/__pageObject';
import ParcelView from '@self/root/src/widgets/content/checkout/common/CheckoutParcel/components/ParcelView/__pageObject';
import PopupBase from '@self/root/src/components/PopupBase/__pageObject';
import ScrollBox from '@self/root/src/components/ScrollBox/__pageObject';


// suites
import closingPopupWithOrdersCompositionList from './closingPopupWithOrdersCompositionList';
import scrollOrdersList from './scrollOrdersList';

export default makeSuite('Попап "Состав заказа".', {
    environment: 'kadavr',
    story: mergeSuites(
        {
            async beforeEach() {
                this.setPageObjects({
                    popupBase: () => this.createPageObject(PopupBase, {
                        root: `${PopupBase.root} [data-auto="cartItemsDetails"]`,
                    }),
                    cartItemsDetails: () => this.createPageObject(CartItemsDetails),
                    orderItemsList: () => this.createPageObject(OrderItemsList, {
                        perent: this.cartItemsDetails,
                    }),
                    editableRecipientCard: () => this.createPageObject(ContactCard, {
                        parent: this.deleteForm,
                    }),
                    parcelView: () => this.createPageObject(ParcelView, {
                        parent: this.addressEditableCard,
                    }),
                    scrollBox: () => this.createPageObject(ScrollBox, {
                        parent: this.addressEditableCard,
                    }),
                });
            },
        },
        prepareSuite(closingPopupWithOrdersCompositionList, {
            meta: {
                id: 'marketfront-5054',
                issue: 'MARKETFRONT-54673',
            },
        }),
        prepareSuite(scrollOrdersList, {
            meta: {
                id: 'marketfront-5055',
                issue: 'MARKETFRONT-54673',
            },
        })
    ),
});
