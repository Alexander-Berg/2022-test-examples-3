const HOST = 'http://cs-mxclassifier.tst.vs.market.yandex.net:34501';

const ROUTE = /\/classify/;

const RESPONSE = {
    offer: [
        {
            category: [
                {
                    category_id: 857707,
                    probability: 0.9627246209035051,
                    rank: 6.829459566211504,
                    override_doubtful_classification: false,
                    confident_top_precision: 0.9822858409914016,
                },
                {
                    category_id: 818965,
                    probability: 0.0,
                    rank: 3.227318809050849,
                    override_doubtful_classification: false,
                    confident_top_precision: 0.0,
                },
                {
                    category_id: 16224971,
                    probability: 0.0,
                    rank: 2.2733631692810983,
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
