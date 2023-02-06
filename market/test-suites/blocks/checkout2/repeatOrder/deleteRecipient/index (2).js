import {
    makeSuite,
    prepareSuite,
    mergeSuites,
} from 'ginny';

// scenarios
import {buildCheckouterBucket} from '@self/root/src/spec/utils/checkouter';

// pageObjects
import RecipientPopup
    from '@self/root/src/widgets/content/checkout/common/CheckoutRecipient/components/Popup/__pageObject/index.touch';
import RecipientList
    from '@self/root/src/widgets/content/checkout/common/CheckoutRecipient/components/Popup/components/RecipientList/__pageObject/index.touch';
import RecipientFormFields from '@self/root/src/components/RecipientForm/__pageObject';
import ContactCard from '@self/root/src/components/Checkout/ContactCard/__pageObject';
import DeleteForm
    from '@self/root/src/widgets/content/checkout/common/CheckoutRecipient/components/Popup/components/DeleteForm/__pageObject/index.touch';

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
                    recipientPopup: () => this.createPageObject(RecipientPopup),
                    recipientList: () => this.createPageObject(RecipientList),
                    recipientFormFields: () => this.createPageObject(RecipientFormFields),
                    deleteForm: () => this.createPageObject(DeleteForm),
                    editableRecipientCard: () => this.createPageObject(ContactCard, {
                        parent: this.deleteForm,
                    }),
                });
            },
        },
        prepareSuite(oneRecipient, {
            meta: {
                id: 'm-touch-3510',
                issue: 'MARKETFRONT-36084',
            },
            params: {
                carts: simpleCarts,
            },
        }),
        prepareSuite(twoRecipients, {
            meta: {
                id: 'm-touch-3509',
                issue: 'MARKETFRONT-36084',
            },
            params: {
                carts: simpleCarts,
            },
        })
    ),
});
