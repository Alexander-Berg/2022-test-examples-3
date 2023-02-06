import {CurrencyType} from 'utilities/currency/CurrencyType';

const exampleAviaVariants = [
    {
        partner: 'aeroflot',
        baggageForward: ['0d1dN'],
        baggageBackward: [],
        charter: null,
        fareCodesForward: ['UNOR'],
        fareCodesBackward: [],
        selfConnect: false,
        tariff: {
            currency: CurrencyType.RUB,
            value: 19226.0,
        },
        routeForward: [
            {
                company: 26,
                companyTariff: 553,
                departure: {
                    local: '2021-12-29T21:30:00',
                    offset: 180,
                    tzName: 'Europe/Moscow',
                },
                arrival: {
                    local: '2021-12-30T02:00:00',
                    offset: 300,
                    tzName: 'Asia/Yekaterinburg',
                },
                from: 9600213,
                to: 9600370,
                number: 'SU 1406',
                operating: {
                    company: 26,
                    number: 'SU 1406',
                },
            },
        ],
        routeBackward: [],
    },
];

export default exampleAviaVariants;
