const HOST = 'http://cs-mxclassifier.tst.vs.market.yandex.net:34501';

const ROUTE = /\/classify/;

const RESPONSE = {
    offer: [
        {
            category: [
                {
                    category_id: 987260,
                    probability: 0.9627246209035051,
                    rank: 0.11824425946139083,
                    override_doubtful_classification: false,
                    confident_top_precision: 0.9822858409914016,
                },
                {
                    category_id: 13626008,
                    probability: 0.0,
                    rank: -1.7183343255663026,
                    override_doubtful_classification: false,
                    confident_top_precision: 0.0,
                },
                {
                    category_id: 90927,
                    probability: 0.0,
                    rank: -2.0427508864568797,
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
