const HOST = 'http://cs-mxclassifier.tst.vs.market.yandex.net:34501';

const ROUTE = /\/classify/;

const RESPONSE = {
    offer: [
        {
            category: [
                {
                    category_id: 658853,
                    probability: 0.9627246209035051,
                    rank: 4.464765175553322,
                    override_doubtful_classification: false,
                    confident_top_precision: 0.9822858409914016,
                },
                {
                    category_id: 13012377,
                    probability: 0.0,
                    rank: 0.92795036008609,
                    override_doubtful_classification: false,
                    confident_top_precision: 0.0,
                },
                {
                    category_id: 15364998,
                    probability: 0.0,
                    rank: 0.39752370731247455,
                    override_doubtful_classification: false,
                    confident_top_precision: 0.0,
                },
            ],
            type: 'DEFAULT',
            honest_mark_departments: [
                {
                    name: 'other',
                    probability: 0.9917707282886941,
                },
                {
                    name: 'boots',
                    probability: 0.0008164693141486464,
                },
                {
                    name: 'drugs',
                    probability: 0.0014911702921930278,
                },
                {
                    name: 'perfume',
                    probability: 0.0038647996945781003,
                },
                {
                    name: 'photo',
                    probability: 0.00023684407610468218,
                },
                {
                    name: 'tires',
                    probability: 0.0005800352752060026,
                },
                {
                    name: 'textile',
                    probability: 0.0012399530590756068,
                },
            ],
        },
    ],
};

module.exports = {
    host: HOST,
    route: ROUTE,
    response: RESPONSE,
    method: 'post',
};
