import preparePassengerContacts from '../preparePassengerContacts';
import createOrder from './__mocks__/order';
import createPassenger from './__mocks__/passenger';

describe('preparePassengerContacts', () => {
    describe('Без записной книжки', () => {
        const order = createOrder();

        beforeEach(() => {
            order.traveler = {
                passengers: [],
                accountPassengers: [],
            };
        });

        describe('Снята галка "Использовать для всех пассажиров"', () => {
            beforeEach(() => {
                order.contacts.useContacts.value = false;
            });

            it('Если не указан телефон или email, то должен вернуть телефон и email из контактов заказа', () => {
                const passenger = createPassenger();

                passenger.emailOrPhone.value = '';

                const {traveler, contacts} = order;

                expect(
                    preparePassengerContacts({
                        storePassenger: passenger,
                        traveler,
                        contacts,
                    }),
                ).toEqual({
                    email: order.contacts.email.value,
                    phone: order.contacts.phone.value,
                });
            });

            it('Если указан email, то должен вернуть указанный email', () => {
                const passenger = createPassenger();

                passenger.emailOrPhone.value = 'ivan@yandex.ru';

                const {traveler, contacts} = order;

                expect(
                    preparePassengerContacts({
                        storePassenger: passenger,
                        traveler,
                        contacts,
                    }),
                ).toEqual({
                    email: 'ivan@yandex.ru',
                });
            });

            it('Если указан телефон, то должен вернуть указанный телефон', () => {
                const passenger = createPassenger();

                passenger.emailOrPhone.value = '+79111111111';

                const {traveler, contacts} = order;

                expect(
                    preparePassengerContacts({
                        storePassenger: passenger,
                        traveler,
                        contacts,
                    }),
                ).toEqual({
                    phone: '+79111111111',
                });
            });
        });

        describe('Стоит галка "Использовать для всех пассажиров"', () => {
            beforeEach(() => {
                order.contacts.useContacts.value = true;
            });

            it('Если указан email, то должен вернуть из контактов заказа', () => {
                const passenger = createPassenger();

                passenger.emailOrPhone.value = 'ivan@yandex.ru';

                const {traveler, contacts} = order;

                expect(
                    preparePassengerContacts({
                        storePassenger: passenger,
                        traveler,
                        contacts,
                    }),
                ).toEqual({
                    email: order.contacts.email.value,
                    phone: order.contacts.phone.value,
                });
            });

            it('Если указан телефон, то должен вернуть из контактов заказа', () => {
                const passenger = createPassenger();

                passenger.emailOrPhone.value = '+79111111111';

                const {traveler, contacts} = order;

                expect(
                    preparePassengerContacts({
                        storePassenger: passenger,
                        traveler,
                        contacts,
                    }),
                ).toEqual({
                    email: order.contacts.email.value,
                    phone: order.contacts.phone.value,
                });
            });
        });
    });

    describe('С заполнением из записной книжки', () => {
        it('Если снята галочка из личного кабинета "Я хочу получать уведомления от РЖД об изменении в расписании поездов", то контакты из ЛК не попадают', () => {
            const order = createOrder();
            const passenger = createPassenger();

            order.traveler.passengers[0].train_notifications_enabled = false;

            const {traveler, contacts} = order;

            expect(
                preparePassengerContacts({
                    storePassenger: passenger,
                    traveler,
                    contacts,
                }),
            ).toEqual({
                email: order.contacts.email.value,
                phone: order.contacts.phone.value,
            });
        });

        describe('Снята галка "Использовать для всех пассажиров"', () => {
            const order = createOrder();

            beforeEach(() => {
                order.traveler.passengers[0].train_notifications_enabled = true;

                order.contacts.useContacts.value = false;
            });

            it('Если не указан телефон или email, то должен вернуть телефон и email из записной книжки', () => {
                const passenger = createPassenger();

                passenger.emailOrPhone.value = '';

                const {traveler, contacts} = order;

                expect(
                    preparePassengerContacts({
                        storePassenger: passenger,
                        traveler,
                        contacts,
                    }),
                ).toEqual({
                    email: order.traveler.passengers[0].email,
                    phone: order.traveler.passengers[0].phone,
                });
            });

            it('Если указан email, то должен вернуть указанный email', () => {
                const passenger = createPassenger();

                passenger.emailOrPhone.value = 'ivan@yandex.ru';

                const {traveler, contacts} = order;

                expect(
                    preparePassengerContacts({
                        storePassenger: passenger,
                        traveler,
                        contacts,
                    }),
                ).toEqual({
                    email: 'ivan@yandex.ru',
                });
            });

            it('Если указан телефон, то должен вернуть указанный телефон', () => {
                const passenger = createPassenger();

                passenger.emailOrPhone.value = '+79111111111';

                const {traveler, contacts} = order;

                expect(
                    preparePassengerContacts({
                        storePassenger: passenger,
                        traveler,
                        contacts,
                    }),
                ).toEqual({
                    phone: '+79111111111',
                });
            });
        });

        describe('Стоит галка "Использовать для всех пассажиров"', () => {
            const order = createOrder();

            beforeEach(() => {
                order.contacts.useContacts.value = true;
            });

            it('Если указан email, то должен вернуть из контактов заказа', () => {
                const passenger = createPassenger();

                passenger.emailOrPhone.value = 'ivan@yandex.ru';

                const {traveler, contacts} = order;

                expect(
                    preparePassengerContacts({
                        storePassenger: passenger,
                        traveler,
                        contacts,
                    }),
                ).toEqual({
                    email: order.contacts.email.value,
                    phone: order.contacts.phone.value,
                });
            });

            it('Если указан телефон, то должен вернуть из контактов заказа', () => {
                const passenger = createPassenger();

                passenger.emailOrPhone.value = '+79111111111';

                const {traveler, contacts} = order;

                expect(
                    preparePassengerContacts({
                        storePassenger: passenger,
                        traveler,
                        contacts,
                    }),
                ).toEqual({
                    email: order.contacts.email.value,
                    phone: order.contacts.phone.value,
                });
            });
        });
    });
});
