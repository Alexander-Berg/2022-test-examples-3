export default {
    data: {
        search: {
            isDeliveryIncluded: false,
            isLocalOffersFirst: false,
            isParametricSearch: false,
            salesDetected: false,
            shopOutlets: 0,
            shops: 1,
            showBlockId: '',
            totalShopsBeforeFilters: 1,
            cpaCount: 0,
            duplicatesHidden: 0,
            groupBy: 'shop',
            total: 1,
            totalOffers: 1,
            totalOffersBeforeFilters: 1,
            totalModels: 0,
            sorts: ['по популярности', 'по цене'],
            filters: ['glprice', 'onstock'],
            results: [
                {
                    id: '3',
                    schema: 'offer',
                },
            ],
        },
    },
    collections: {
        sort: {
            'по цене': {
                text: 'по цене',
                options: [
                    {
                        id: 'aprice',
                        type: 'asc',
                    },
                    {
                        id: 'dprice',
                        type: 'desc',
                    },
                ],
            },
            'по популярности': {
                text: 'по популярности',
            },
        },
        filterValue: {
            glprice_found: {
                max: '10',
                min: '5',
                initialMax: '10',
                initialMin: '5',
                id: 'found',
            },
            onstock_0: {
                value: '0',
                initialFound: 4,
                found: 4,
                checked: true,
            },
            onstock_1: {
                value: '1',
                initialFound: 4,
                found: 4,
            },
        },
        filter: {
            glprice: {
                id: 'glprice',
                type: 'number',
                name: 'Цена',
                subType: '',
                kind: 2,
                values: ['glprice_found'],
            },
            onstock: {
                id: 'onstock',
                type: 'boolean',
                name: 'В продаже',
                subType: '',
                kind: 2,
                values: ['onstock_0', 'onstock_1'],
            },
        },
    },
};
