import { CurrencyAvailable } from '@yandex-turbo/components/Cost/Currency/Currency';
import { getDelivery } from '../turbojsonParser/common/parseDeliveryData';

describe('GetDelivery', () => {
    describe('Возвращает верный объект', () => {
        const deviceDetect = {
            BrowserVersion: '0013.0000.0003',
            OSFamily: 'iOS',
            BrowserName: 'MobileSafari',
            BrowserVersionRaw: '13.0.3',
            OSVersion: '13.2.3',
            BrowserEngine: 'WebKit',
            OSName: '',
            OSVersionRaw: '',
            BrowserBase: '',
        };
        const expFlags = {};
        let data = {
            courier: [{
                free_from: null,
                currency: 'RUB' as CurrencyAvailable,
                min_delivery_period: 1,
                max_delivery_period: 2,
                price: 450,
            }],
            pickup: [],
            mail: [],
        };

        it('Нет цены', () => {
            //@ts-ignore
            data.courier[0].price = null;

            const expected = [
                {
                    label: 'Курьером',
                    list: [{
                        period: '1–2 дня',
                        name: undefined,
                        price: undefined,
                        workTime: undefined,
                    }],
                },
            ];

            expect(getDelivery(data, deviceDetect, expFlags)).toStrictEqual(expected);
        });

        it('Нет максимальной даты доставки', () => {
            //@ts-ignore
            data.courier[0].max_delivery_period = null;

            const expected = [
                {
                    label: 'Курьером',
                    list: [{
                        period: '1 день',
                        name: undefined,
                        price: undefined,
                        workTime: undefined,
                    }],
                },
            ];

            expect(getDelivery(data, deviceDetect, expFlags)).toStrictEqual(expected);
        });

        it('Нет максимальной даты доставки, а минимальная 0 дней', () => {
            data.courier[0].min_delivery_period = 0;

            const expected = [
                {
                    label: 'Курьером',
                    list: [{
                        period: 'сегодня',
                        name: undefined,
                        price: undefined,
                        workTime: undefined,
                    }],
                },
            ];

            expect(getDelivery(data, deviceDetect, expFlags)).toStrictEqual(expected);
        });
    });
});
