const HOST = 'http://cs-mxclassifier.tst.vs.market.yandex.net:34501';

const ROUTE = /\/classify/;

const RESPONSE = {
    offer: [
        {
            category: [
                {
                    category_id: 91033,
                    probability: 0.9627246209035051,
                    rank: 10.582659440961415,
                    override_doubtful_classification: false,
                    confident_top_precision: 0.9822858409914016,
                },
                {
                    category_id: 16309374,
                    probability: 0.0,
                    rank: 6.013475313600048,
                    override_doubtful_classification: false,
                    confident_top_precision: 0.0,
                },
                {
                    category_id: 91011,
                    probability: 0.0,
                    rank: 2.119135102365926,
                    override_doubtful_classification: false,
                    confident_top_precision: 0.0,
                },
            ],
            type: 'DEFAULT',
            honest_mark_departments: [
                { name: 'other', probability: 0.9917707282886941 },
                { name: 'boots', probability: 8.164693141486464e-4 },
                { name: 'drugs', probability: 0.0014911702921930278 },
                { name: 'perfume', probability: 0.0038647996945781003 },
                { name: 'photo', probability: 2.3684407610468218e-4 },
                { name: 'tires', probability: 5.800352752060026e-4 },
                { name: 'textile', probability: 0.0012399530590756068 },
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
