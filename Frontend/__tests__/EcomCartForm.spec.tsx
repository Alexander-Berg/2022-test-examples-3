// @ts-nocheck
import * as React from 'react';
import { shallow } from 'enzyme';
import { EcomCartForm } from '../EcomCartForm';
import { Status } from '../types';
import { IUserAddressTypes } from '../../../types/IUser';

describe('EcomCartForm', () => {
    const state = {
        isFocused: false,
        modalVisible: false,
        loadingFields: {
            address: Status.SuccessDegradation,
            delivery: Status.Success,
        },
        formFields: {
            name: {
                type: 'input',
                placeholder: 'Name',
                required: false,
                value: '',
            },
            email: {
                type: 'input',
                placeholder: 'Email',
                required: true,
                value: 'test@example.com',
            },
        },
        formLines: [
            {
                uniq: 'name',
            },
            {
                uniq: 'email',
            },
        ],
    };

    describe('recalcStickyPanelHiddenWithState()', () => {
        it('возвращает false, когда нужно показать залипающую панель', () => {
            expect(EcomCartForm.recalcStickyPanelHiddenWithState(state)).toBeFalsy();
        });

        it('возвращает true, если есть элемент в фокусе', () => {
            state.isFocused = true;

            expect(EcomCartForm.recalcStickyPanelHiddenWithState(state)).toBeTruthy();
        });

        it('возвращает true, если открыта модалка', () => {
            state.modalVisible = true;

            expect(EcomCartForm.recalcStickyPanelHiddenWithState(state)).toBeTruthy();
        });

        it('возвращает true, если поле не загрузилось', () => {
            state.loadingFields.address = Status.Loading;

            expect(EcomCartForm.recalcStickyPanelHiddenWithState(state)).toBeTruthy();
        });

        it('возвращает true, если поле загрузилось с ошибкой', () => {
            state.loadingFields.address = Status.Error;

            expect(EcomCartForm.recalcStickyPanelHiddenWithState(state)).toBeTruthy();
        });

        it('возвращает true, если не заполнено обязательное поле', () => {
            state.formFields.email.value = '';

            expect(EcomCartForm.recalcStickyPanelHiddenWithState(state)).toBeTruthy();
        });
    });

    describe('getSubmitData()', () => {
        const products = {
            '3242424234': {
                id: '3242424234',
                name: 'GoPro miniUSB кабель для подкл. к ТV ACMPS-301',
                href: '/turbo/spideradio.github.io?stub=productpage/product-1.json&exp_flags=platform=touch',
                meta: 'CiBC2hxN7fyAthVvJbZoRD9TpcLvOSyQVG6cD9VjdhOODxKFAgj3wp3oBRL8AQoKMzI0MjQyNDIzNBI+R29Qcm8gbWluaVVTQiDQutCw0LHQtdC70Ywg0LTQu9GPINC/0L7QtNC60LsuINC6INCiViBBQ01QUy0zMDEaPy90dXJibz9zdHViPXByb2R1Y3RwYWdlL3Byb2R1Y3QtMS5qc29uJmV4cF9mbGFncz1wbGF0Zm9ybT10b3VjaCJeClZodHRwOi8vYXZhdGFycy1pbnQubWRzdC55YW5kZXgubmV0L2dldC10dXJiby81MTUwLzJhMDAwMDAxNjdmNDQwMzkwM2IzYjA0OTk5ZjRjNTY1YWFiMBDYBBjYBCoEMTc5MDoDUlVSQgI1JQ==',
                price: {
                    currencyId: 'RUR',
                    value: 1790
                },
                isAvailable: true,
                thumb: 'https://avatars.mds.yandex.net/get-turbo/2393222/rth4af83c47822266e10d328db548983877/'
            }
        };
        const cartItems = [
            {
                id: '3242424234',
                count: 1,
                product: {
                    id: '3242424234',
                    thumb: 'https://avatars.mds.yandex.net/get-turbo/2393222/rth4af83c47822266e10d328db548983877/max_g480_c4_r1x1_pd20',
                    price: {
                        currencyId: 'RUR',
                        value: 1790
                    },
                    description: 'GoPro miniUSB кабель для подкл. к ТV ACMPS-301',
                    isAvailable: true
                }
            }
        ];
        const formFields = {
            name: {
                type: 'input',
                label: 'ФИО',
                placeholder: 'Фёдоров Иван',
                required: true,
                maxLength: 100,
                name: 'name',
                value: 'Фёдоров Иван',
                invalid: false
            },
            customer_phone: {
                type: 'input',
                label: 'Телефон',
                required: true,
                maxLength: 20,
                name: 'customer_phone',
                validation: 'phone',
                value: '+79061234567',
                invalid: false
            },
            customer_email: {
                type: 'input',
                label: 'Email',
                required: true,
                maxLength: 254,
                name: 'customer_email',
                validation: 'email',
                value: 'mail@yandex.ru',
                invalid: false
            },
            delivery: {
                type: 'radio',
                label: 'Способ доставки',
                name: 'delivery',
                options: [
                    {
                        freeFrom: 5000,
                        checked: true,
                        price: 300,
                        value: 'courier_0',
                        label: 'Курьером',
                        currencyId: 'RUB',
                        meta: '1-2 дня, Внутри МКАД',
                        type: 'COURIER'
                    },
                    {
                        freeFrom: 10000,
                        price: 500,
                        value: 'courier_1',
                        label: 'Курьером',
                        currencyId: 'RUB',
                        meta: '2-5 дней, За МКАДом',
                        type: 'COURIER'
                    },
                    {
                        price: 0,
                        value: 'pickup_0',
                        label: 'Самовывоз',
                        currencyId: 'RUB',
                        meta: 'сегодня, Льва Толстого 16, ',
                        type: 'PICKUP'
                    }
                ],
                value: 'courier_0'
            },
            shipping_address: {
                type: 'textarea',
                label: 'Адрес доставки',
                placeholder: 'Город, улица, дом, квартира',
                required: true,
                maxLength: 200,
                name: 'shipping_address',
                value: 'Москва, ул. Льва Толстого, д. 16',
                invalid: false
            }
        };

        const userData = {
            addresses: [{
                building: '16',
                country: 'Россия',
                locality: 'Москва',
                street: 'ул. Льва Толстого',
                regionId: '1',
                type: IUserAddressTypes.OTHER
            }],
        };

        it('Должен удалять символы нулевой длины', () => {
            formFields.name.value += '\u200B';
            formFields.customer_phone.value += '\u200C';
            formFields.customer_email.value += '\u200D';
            formFields.shipping_address.value += '\uFEFF';

            const cartForm = shallow(<EcomCartForm
                shopId="spideradio.github.io"
                apiUrl="/"
                products={products}
                formFields={formFields}
                cartItems={cartItems}
                localStorageId="turbo-app-ecom--spideradio.github.io"
                setPaymentInProgress={jest.fn()}
                showToastAction={jest.fn()}
                hideToastAction={jest.fn()}
                isIos={false}
                isSSROnly={false}
                tld="ru"
                pageType="cart"
                changeVisibilityBottomBar={jest.fn()}
                submitCart={jest.fn()}
                withDynamicDelivery={false}
                marketDeliveryExp={false}
                citySuggestExp={false}
                fresherExp={false}
                expFlags={}
                user={userData}
            />);

            const submitData = cartForm.instance().getSubmitData(formFields);
            expect(submitData).toEqual({
                payment_start: true,
                name: 'Фёдоров Иван',
                customer_phone: '+7 906 123-45-67',
                customer_email: 'mail@yandex.ru',
                delivery: 'courier_0',
                shipping_address: 'Москва, ул. Льва Толстого, д. 16'
            });
        });
    });
});
