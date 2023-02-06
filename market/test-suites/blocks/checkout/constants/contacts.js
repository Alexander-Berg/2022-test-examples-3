const HSCH_CONTACT = {
    id: 'hschContact',
    contactId: 'hschContact',
    nameAndFamily: 'Тест Кейс',
    email: 'marketfront-4894@testpalm.ru',
    phone: '89123456789',
    recipientFullInfo: 'Тест Кейс\nmarketfront-4894@testpalm.ru, 89123456789',
    fullDeliveryInfo: 'Код активации и инструкция придут на marketfront-4894@testpalm.ru',
    recipient: {
        phone: '89123456789',
        email: 'marketfront-4894@testpalm.ru',
        name: 'Тест Кейс',
        lastName: 'Тест',
        firstName: 'Кейс',
    },
};

const DEFAULT_CONTACT = {
    id: 'defaultContact',
    contactId: 'defaultContact',
    recipient: 'Вася Пупкин',
    lastName: 'Пупкин',
    firstName: 'Вася',
    email: 'pupochek@yandex.ru',
    phone: '89876543210',
    phoneNum: '89876543210',
};

const CONTACTS = {
    HSCH_CONTACT,
    DEFAULT_CONTACT,
};

export default CONTACTS;
