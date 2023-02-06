import getDeliveryInfo from './get-delivery-info';

const testCases = [
    {
        title: 'free delivery',
        input: {
            brief: 'в Москву из Новосибирска',
            cityFrom: 'Новосибирска',
            cityTo: 'Москву',
            delivery: true,
            free: true,
            from: 'из Новосибирска',
            pickup: false,
        },
        output: 'Бесплатно',
    },
    {
        title: 'from city',
        input: {
            brief: 'в Москву из Новосибирска',
            cityFrom: 'Новосибирска',
            cityTo: 'Москву',
            delivery: true,
            free: false,
            from: 'из Новосибирска',
            pickup: false,
        },
        output: 'из Новосибирска',
    },
    {
        title: 'actual price',
        input: {
            brief: 'в Москву из Новосибирска',
            cityFrom: 'Новосибирска',
            cityTo: 'Москву',
            delivery: true,
            free: false,
            pickup: false,
            price: {
                currencyCode: 'RUR',
                currencyName: 'руб.',
                value: '250',
            },
        },
        output: '250 руб.',
    },
    {
        title: 'without delivery',
        input: {
            brief: 'в Москву из Новосибирска',
            cityFrom: 'Новосибирска',
            cityTo: 'Москву',
            delivery: false,
            free: false,
            pickup: false,
        },
        output: 'Не производится',
    },
];

describe('getDeliveryInfo', () => {
    testCases.forEach((tc) => {
        const { input, output, title } = tc;

        test(`${title} => '${output}'`, () => {
            expect(getDeliveryInfo(input)).toBe(output);
        });
    });
});
