export default {
    id: 12345,
    slug: 'mama-ama-new',
    isNew: true,
    categories: [{
        entity: 'category',
        slug: 'mobilnye-telefony',
        name: 'Мобильные телефоны',
        id: 91491,
    }],
    navnodes: [{
        entity: 'navnode',
        slug: 'mobilnye-telefony',
        id: 91491,
    }],
    reasonsToBuy: [
        {
            value: 50.06109238,
            type: 'statFactor',
            id: 'bought_n_times',
        },
        {
            value: 0.8828828931,
            type: 'consumerFactor',
            id: 'customers_choice',
        },
        {
            value: 3976,
            type: 'statFactor',
            id: 'viewed_n_times',
        },
        {
            value: 1,
            type: 'statFactor',
            id: 'hype_goods',
        },
    ],
};
