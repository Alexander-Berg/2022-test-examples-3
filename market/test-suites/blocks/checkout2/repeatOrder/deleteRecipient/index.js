import {
    makeSuite,
    prepareSuite,
    mergeSuites,
} from 'ginny';

// scenarios
import {buildCheckouterBucket} from '@self/root/src/spec/utils/checkouter';

// pageObjects
import RecipientForm
    from '@self/root/src/widgets/content/checkout/common/CheckoutRecipient/components/Popup/components/RecipientForm/__pageObject';
import RecipientList
    from '@self/root/src/widgets/content/checkout/common/CheckoutRecipient/components/Popup/components/RecipientList/__pageObject';
import RecipientFormFields from '@self/root/src/components/RecipientForm/__pageObject';
import ContactCard from '@self/root/src/components/Checkout/ContactCard/__pageObject';
import DeleteForm
    from '@self/root/src/widgets/content/checkout/common/CheckoutRecipient/components/Popup/components/DeleteForm/__pageObject';
import EmptyRecipientCard
    from '@self/root/src/widgets/content/checkout/common/CheckoutRecipient/components/EmptyContactCard/__pageObject';

// mocks
import * as kettle from '@self/root/src/spec/hermione/kadavr-mock/report/kettle';

// suites
import oneRecipient from './oneRecipient';
import twoRecipients from './twoRecipients';

const simpleCarts = [
    buildCheckouterBucket({
        items: [{
            skuMock: kettle.skuMock,
            offerMock: kettle.offerMock,
            count: 1,
        }],
    }),
];

export default makeSuite('Удаление активного пресета в попапе "Получатель".', {
    environment: 'kadavr',
    story: mergeSuites(
        {
            async beforeEach() {
                this.setPageObjects({
                    emptyRecipientCard: () => this.createPageObject(EmptyRecipientCard, {
                        parent: this.confirmationPage,
                    }),
                    recipientList: () => this.createPageObject(RecipientList),
                    recepientForm: () => this.createPageObject(RecipientForm),
                    recipientFormFields: () => this.createPageObject(RecipientFormFields, {
                        parent: this.recipientForm,
                    }),
                    deleteForm: () => this.createPageObject(DeleteForm),
                    editableRecipientCard: () => this.createPageObject(ContactCard, {
                        parent: this.deleteForm,
                    }),
                });
            },
        },
        prepareSuite(oneRecipient, {
            meta: {
                id: 'marketfront-4431',
                issue: 'MARKETFRONT-36084',
            },
            params: {
                carts: simpleCarts,
            },
        }),
        prepareSuite(twoRecipients, {
            meta: {
                id: 'marketfront-4432',
                issue: 'MARKETFRONT-36084',
            },
            params: {
                carts: simpleCarts,
            },
        })
    ),
});
