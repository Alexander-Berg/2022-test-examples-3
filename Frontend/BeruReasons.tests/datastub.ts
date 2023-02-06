import { IReasonsTypes } from '@yandex-turbo/applications/beru.ru/interfaces';

export const defaultReasonsToBuy = [
    {
        factor_name: 'безопасность модели',
        type: 'consumerFactor',
        factor_id: '797',
        value: 0.9645635486,
        factor_priority: '1',
        id: 'best_by_factor',
    },
    {
        factor_name: 'Скорость нагрева',
        type: 'consumerFactor',
        factor_id: '795',
        value: 0.8387930989,
        factor_priority: '2',
        id: 'best_by_factor',
    },
    {
        value: 5.285714149,
        type: 'consumerFactor',
        id: 'bestseller',
    },
    {
        value: 37,
        type: 'statFactor',
        id: 'bought_n_times',
    },
    {
        value: 0.9235569239,
        type: 'consumerFactor',
        id: 'customers_choice',
    },
    {
        value: 2505,
        type: 'statFactor',
        id: 'viewed_n_times',
    },
];

export const reasonsToProps = [
    {
        icon: 'customersChoice',
        id: IReasonsTypes.CUSTOMERS_CHOICE,
        info: 'покупатели хвалят его в своих отзывах',
        title: '92% рекомендуют',
    },
    {
        icon: 'bestByFactor',
        id: IReasonsTypes.BEST_BY_FACTOR,
        info: 'эти характеристики получили отличные оценки',
        title: 'Нравится безопасность модели, скорость нагрева',
    },
    {
        icon: 'boughtNTimes',
        id: IReasonsTypes.BOUGHT_N_TIMES,
        info: 'за последние 2 месяца',
        title: '37 человек купили',
    },
];
