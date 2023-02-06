module.exports = {
    type: 'snippet',
    data_stub: {
        num: 0,
        construct: {
            type: 'distance_fact',
            data: {
                p2: {
                    detail: {
                        capital_name: null,
                        region_id: 2,
                        region_name: 'Санкт-Петербург',
                        capital_id: null
                    },
                    longitude: 30.315868,
                    latitude: 59.939095,
                    lr: 2
                },
                distance: 634621.0354,
                p1: {
                    detail: {
                        capital_name: null,
                        region_id: 213,
                        region_name: 'Москва',
                        capital_id: null
                    },
                    longitude: 37.620393,
                    latitude: 55.75396,
                    lr: 213
                }
            },
            baobab: {
                path: '/wiz/distance_fact'
            },
            is_shareable: true
        }
    }
};
