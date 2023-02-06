import userFormData
    from '@self/root/src/spec/hermione/configs/checkout/formData/user-prepaid';

const addressCardText =
    'Москва, Усачёва улица, д. 52, 12' +
    '\n' +
    '1 подъезд, 15 этаж, домофон 12test, "Тестирование"';
const dateText = 'с 23 февраля по 8 марта';
const parcelCardTitleText =
    'Доставка курьером 23 февраля – 8 марта•250₽';
const timeText = '16:00-20:00';
const recipientCardText =
    `${userFormData.recipient.name}\n` +
    `${userFormData.recipient.email}` +
    ', ' +
    `${userFormData.recipient.phone}`;
const paymentCardText =
    'Способ оплаты\nИзменить\nНаличными при получении';

const EXPECT_ADDRESS_CARD_TEXT =
    `Текст карточки адреса должен быть - ${addressCardText}`;
const EXPECT_DATE_TEXT =
    `Текст селекта интервала даты должен быть - ${dateText}`;
const EXPECT_PARCEL_CARD_TEXT =
    `Заголовок на карточке товара должен быть - ${parcelCardTitleText}`;
const EXPECT_TIME_TEXT =
    `Текст селекта интервала времени должен быть - ${timeText}`;
const EXPECT_RECIPIENT_CARD_TEXT =
    `Текст на карточке получателя должен быть - ${recipientCardText}`;
const EXPECT_PAYMENT_CARD_TEXT =
    `Текст на карточке оплаты должен быть - ${paymentCardText}`;

export const addressCardCheck = {
    name: 'Проверяем карточку адреса',
    async func() {
        return this.addressCard.getText().should.eventually.to.be.equal(
            addressCardText,
            EXPECT_ADDRESS_CARD_TEXT
        );
    },
};

export const dateCheck = {
    name: 'Проверяем интервал даты.',
    async func() {
        return this.dateSelect.getText()
            .should.eventually.to.be.include(
                dateText,
                EXPECT_DATE_TEXT
            );
    },
};

export const parcelCardTitleCheck = {
    name: 'Проверяем заголовок карточки товара.',
    async func() {
        return this.editGroupedParcelCard.getTitle()
            .should.eventually.to.be.equal(
                parcelCardTitleText,
                EXPECT_PARCEL_CARD_TEXT
            );
    },
};

export const timeCheck = {
    name: 'Проверяем интервал времени.',
    async func() {
        return this.timeSelect.getText()
            .should.eventually.to.be.include(
                timeText,
                EXPECT_TIME_TEXT
            );
    },
};

export const recipientCardCheck = {
    name: 'Проверяем карточку получателя.',
    async func() {
        return this.checkoutRecipient.getContactText()
            .should.eventually.to.be.equal(
                recipientCardText,
                EXPECT_RECIPIENT_CARD_TEXT
            );
    },
};

export const paymentCardCheck = {
    name: 'Проверяем карточку оплаты.',
    async func() {
        return this.editPaymentOption.getText()
            .should.eventually.to.be.equal(
                paymentCardText,
                EXPECT_PAYMENT_CARD_TEXT
            );
    },
};
