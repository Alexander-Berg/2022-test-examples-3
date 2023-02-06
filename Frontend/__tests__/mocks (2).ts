import { IMarketDeliveryProps } from '@yandex-turbo/components/MarketDelivery/MarketDelivery';

export const withDayBenefit: IMarketDeliveryProps = {
    rows: [
        {
            items: [
                {
                    text: '+350 ₽ доставка',
                },
                {
                    text: 'сегодня',
                    isBenefit: true,
                },
            ],
        },
        {
            items: [
                {
                    text: 'Доступен самовывоз',
                },
            ],
        },
    ],
};

export const withoutBenefits: IMarketDeliveryProps = {
    rows: [
        {
            items: [
                {
                    text: '+300 ₽ доставка',
                },
                {
                    text: '2-4 дня',
                },
            ],
        },
        {
            items: [
                {
                    text: 'Доступен самовывоз',
                },
            ],
        },
    ],
};

export const withFreeCourierBenefit: IMarketDeliveryProps = {
    rows: [
        {
            items: [
                {
                    text: 'Бесплатная доставка',
                    isBenefit: true,
                },
                {
                    text: 'завтра',
                    isBenefit: true,
                },
            ],
        },
        {
            items: [
                {
                    text: 'Доступен самовывоз',
                },
            ],
        },
    ],
};

export const pickupMultiOptions: IMarketDeliveryProps = {
    rows: [
        {
            items: [
                { text: 'Бесплатная доставка', isBenefit: true },
                { text: '2 дня', isBenefit: false },
                { text: '351\u00A0пункт Boxberry' },
            ],
        },
        {
            items: [
                { text: 'Бесплатная доставка', isBenefit: true },
                { text: '2 дня', isBenefit: false },
                { text: '30\u00A0пунктов Стриж Почтоматы' },
            ],
        },
        {
            items: [
                { text: 'Бесплатная доставка', isBenefit: true },
                { text: '3 дня', isBenefit: false },
                { text: '152\u00A0пункта СДЭК' },
            ],
        },
        {
            items: [
                { text: 'Бесплатная доставка', isBenefit: true },
                { text: '4 дня', isBenefit: false },
                { text: '429\u00A0пунктов DPD' },
            ],
        },
    ],
};
