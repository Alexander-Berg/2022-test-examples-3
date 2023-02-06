import {
    prepareSuite,
    makeSuite,
    mergeSuites,
} from 'ginny';

import {
    prepareCheckouterPageWithCartsForRepeatOrder,
    addPresetForRepeatOrder,
} from '@self/root/src/spec/hermione/scenarios/checkout';

import {buildCheckouterBucket} from '@self/root/src/spec/utils/checkouter';
import * as kettle from '@self/root/src/spec/hermione/kadavr-mock/report/kettle';
import {region} from '@self/root/src/spec/hermione/configs/geo';

import RecipientList from '@self/root/src/widgets/content/checkout/common/CheckoutRecipient/components/Popup/components/RecipientList/__pageObject';
import RecipientForm from '@self/root/src/widgets/content/checkout/common/CheckoutRecipient/components/Popup/components/RecipientForm/__pageObject';
import RecipientFormFields from '@self/root/src/components/RecipientForm/__pageObject';

import {ADDRESSES, CONTACTS} from '../../constants';
import validationNameField from './validationNameField';
import validationEmailField from './validationEmailField';
import validationPhoneField from './validationPhoneField';

export default makeSuite('Валидация полей формы получателя с последующим сохранением внесенных изменений', {
    id: 'marketfront-5013',
    issue: 'MARKETFRONT-54580',
    feature: 'Валидация полей формы получателя с последующим сохранением внесенных изменений',
    params: {
        region: 'Регион',
        isAuthWithPlugin: 'Авторизован ли пользователь',
    },
    defaultParams: {
        region: region['Москва'],
        isAuth: false,
    },
    environment: 'kadavr',
    story: mergeSuites(
        {
            async beforeEach() {
                this.setPageObjects({
                    recipientList: () => this.createPageObject(RecipientList),
                    recepientForm: () => this.createPageObject(RecipientForm),
                    recipientFormFields: () => this.createPageObject(RecipientFormFields, {
                        parent: this.recipientForm,
                    }),
                });

                const carts = [
                    buildCheckouterBucket({
                        items: [{
                            skuMock: kettle.skuMock,
                            offerMock: kettle.offerMock,
                            count: 1,
                        }],
                    }),
                ];

                await this.browser.yaScenario(
                    this,
                    addPresetForRepeatOrder,
                    {
                        address: ADDRESSES.MOSCOW_ADDRESS,
                        contact: CONTACTS.DEFAULT_CONTACT,
                    }
                );

                await this.browser.yaScenario(
                    this,
                    prepareCheckouterPageWithCartsForRepeatOrder,
                    {
                        carts,
                        options: {
                            region: this.params.region,
                            checkout2: true,
                        },
                    }
                );
            },
        },
        prepareSuite(validationNameField),

        prepareSuite(validationEmailField),

        prepareSuite(validationPhoneField)
    ),
});
