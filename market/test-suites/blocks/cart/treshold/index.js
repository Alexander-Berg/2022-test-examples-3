import {
    makeSuite,
    prepareSuite,
} from 'ginny';
import tresholdSuite from './tresholdSuite';

module.exports = makeSuite('Уведомление об условиях доставки', {
    environment: 'kadavr',
    story: {
        'При стоимости товаров в корзине меньше трешхолда': {
            'и при наличии у пользователя подписки YaPlus.': prepareSuite(tresholdSuite,
                {
                    meta: {
                        id: 'marketfront-5271',
                        issue: 'MARKETFRONT-47459',
                    },
                    params: {
                        isOrderMoreThanTreshold: false,
                        hasYaPlus: true,
                    },
                }),
            'и при отсутствии у пользователя подписки YaPlus.': prepareSuite(tresholdSuite,
                {
                    meta: {
                        id: 'marketfront-5268',
                        issue: 'MARKETFRONT-47459',
                    },
                    params: {
                        isOrderMoreThanTreshold: false,
                        hasYaPlus: false,
                    },
                }),
        },

        'При стоимости товаров в корзине больше трешхолда': {
            'и при наличии у пользователя подписки YaPlus.': prepareSuite(tresholdSuite,
                {
                    meta: {
                        id: 'marketfront-5272',
                        issue: 'MARKETFRONT-47459',
                    },
                    params: {
                        isOrderMoreThanTreshold: true,
                        hasYaPlus: true,
                    },
                }),
            'и при отсутствии у пользователя подписки YaPlus.': prepareSuite(tresholdSuite,
                {
                    meta: {
                        id: 'marketfront-5267',
                        issue: 'MARKETFRONT-47459',
                    },
                    params: {
                        isOrderMoreThanTreshold: true,
                        hasYaPlus: false,
                    },
                }),
        },
    },
});
