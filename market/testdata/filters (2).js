/* eslint-disable */
module.exports = {
    search: {
        filters: [
            {
                id: 'glprice'
            },
            {
                id: 'offer-shipping',
                type: 'boolean',
                name: 'Способ доставки',
                subType: '',
                kind: 2,
                hasBoolNo: true,
                values: [
                    {
                        found: 2,
                        value: 'delivery'
                    },
                    {
                        found: 2,
                        value: 'pickup'
                    },
                    {
                        found: 1,
                        value: 'postomat'
                    },
                    {
                        value: 'store'
                    }
                ]
            }
        ]
    }
};
