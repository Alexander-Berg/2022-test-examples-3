import json

from context import StatsCalcBaseTestCase
from market.idx.pylibrary.offer_flags.flags import OfferFlags


# for this classifier_magic_id hasrequiredparams is set false (see make_genlog.cpp)
bad_classifier_id = "32d29bfbe1e816109ca9b45696b6a077"
good_classifier_id = "ad1d66153519254f804f33eda7868cbd"

# for this cluster_id there are no required picture: see cluster_pictures.pbuf.sn
bad_cluster_id = 1005899581
good_cluster_id = 1005899582


class TestModelClusterStats(StatsCalcBaseTestCase):
    def setUp(self):
        super(TestModelClusterStats, self).setUp()

        self.gls = [
            # 1. Models

            # valid genlogs
            # model with id = 3 is a group model for the one with model_id = 2 (see data/model_group.csv)
            # for model_id = 2 [14, 14, 16, 18, 1000] => median price is 16
            # for model_id = 3 [10, 12, 14, 14, 16, 18, 1000] => median price is 14
            {
                'model_id': 3,
                'binary_price': '12 1 0 RUR RUR',
                'flags': 0
            },
            {
                'model_id': 2,
                'binary_price': '14 1 0 RUR RUR',
                'flags': 0
            },
            {
                'model_id': 2,
                'binary_price': '14 1 0 RUR RUR',
                'flags': 0
            },
            {
                'model_id': 2,
                'binary_price': '16 1 0 RUR RUR',
                'flags': 0
            },
            {
                'model_id': 2,
                'binary_price': '18 1 0 RUR RUR',
                'flags': 0
            },
            {
                'model_id': 2,
                'binary_price': '1000 1 0 RUR RUR',
                'flags': 0
            },
            {
                'model_id': 3,
                'binary_price': '10 1 0 RUR RUR',
                'flags': OfferFlags.ADULT.value
            },

            # invalid genlogs - should be skipped
            # no model_id
            {
                'binary_price': '14 1 0 RUR RUR'
            },
            # no binary_price
            {
                'model_id': 34
            },

            # 2. Clusters

            # valid genlogs
            # cluster with id = 10 has prices [2, 3, 5], so the median should be 3
            {
                'cluster_id': good_cluster_id,
                'binary_price': '2 1 0 RUR RUR',
                'classifier_magic_id': good_classifier_id,
                'flags': OfferFlags.ADULT.value
            },
            {
                'cluster_id': good_cluster_id,
                'binary_price': '3 1 0 RUR RUR',
                'classifier_magic_id': good_classifier_id,
                'flags': OfferFlags.ADULT.value
            },
            {
                'cluster_id': good_cluster_id,
                'binary_price': '5 1 0 RUR RUR',
                'classifier_magic_id': good_classifier_id
            },

            # invalid genlogs - should be skipped
            # no cluster_id
            {
                'binary_price': '5 1 0 RUR RUR',
                'classifier_magic_id': good_classifier_id
            },
            # no binary_price
            {
                'cluster_id': good_cluster_id,
                'classifier_magic_id': good_classifier_id
            },
            # hasrequiredparams = False
            {
                'cluster_id': good_cluster_id,
                'binary_price': '5 1 0 RUR RUR',
                'classifier_magic_id': bad_classifier_id
            },
            # haspictures = False
            {
                'cluster_id': bad_cluster_id,
                'binary_price': '1 1 0 RUR RUR',
                'classifier_magic_id': good_classifier_id
            },
            # has both non-zero cluster id and model id
            {
                'cluster_id': good_cluster_id,
                'model_id': 2,
                'binary_price': '1200 1 0 RUR RUR',
                'classifier_magic_id': good_classifier_id
            },
            # contex, skipped
            {
                'model_id': 2,
                'binary_price': '1000 1 0 RUR RUR',
                'flags': 0,
                'contex_info': {
                    'original_msku_id': 100,
                },
            },
            # direct, skipped
            {
                'model_id': 2,
                'binary_price': '1001 1 0 RUR RUR',
                'flags': OfferFlags.IS_DIRECT.value
            },
            # Lavka, skipped
            {
                'model_id': 2,
                'binary_price': '1001 1 0 RUR RUR',
                'flags': OfferFlags.IS_LAVKA.value
            },
            # Eda, skipped
            {
                'model_id': 2,
                'binary_price': '1001 1 0 RUR RUR',
                'flags': OfferFlags.IS_EDA_RESTAURANTS.value
            },
        ]

    def test_model_stats(self):
        self.run_stats_calc(
            'ModelGeoStats',
            json.dumps(self.gls)
        )

        expected = [
            {'model_id': 2, 'median_price': 16, 'flags': 0},
            {'model_id': 3, 'median_price': 14, 'flags': 0},
            {'model_id': good_cluster_id, 'median_price': 3,  'flags': OfferFlags.ADULT.value},
        ]

        file_path = self.tmp_file_path('model_stats.mmap')
        model_stats_json = self.get_stats_from_mmap(file_path)

        self.assertTrue('model_stats' in model_stats_json)
        self.assertEquals(len(expected), len(model_stats_json['model_stats']))
        self.assertTrue(all([expected_item in model_stats_json['model_stats'] for expected_item in expected]))

    def test_model_flags(self):
        def make_gl_record(model_id, classifier_id, price, old_price, flags):
            gl_record = dict()
            gl_record["classifier_magic_id"] = classifier_id
            gl_record["model_id"] = model_id
            gl_record["flags"] = flags
            gl_record["binary_price"] = '%d 1 0 RUR RUR' % price
            gl_record["binary_oldprice"] = '%d 1 0 RUR RUR' % old_price
            return gl_record

        def check_stats(model_stats, model_id, flags, median_price):
            self.assertTrue({u'median_price': median_price, u'model_id': model_id, u'flags': flags} in model_stats['model_stats'])

        records = [
            make_gl_record(120, good_classifier_id, 1000, 1800, OfferFlags.ADULT.value),
            make_gl_record(120, good_classifier_id, 2000, 2800, OfferFlags.ADULT.value),
            make_gl_record(120, good_classifier_id, 1800, 1900, OfferFlags.ADULT.value),

            make_gl_record(125, good_classifier_id, 1000, 1800, OfferFlags.ADULT.value),
            make_gl_record(125, good_classifier_id, 2700, 2800, OfferFlags.ADULT.value),
            make_gl_record(125, good_classifier_id, 1600, 1900, 0),

            make_gl_record(130, good_classifier_id, 1000, 1800, OfferFlags.ADULT.value),
            make_gl_record(130, good_classifier_id, 2700, 2800, 0),
            make_gl_record(130, good_classifier_id, 1600, 1900, 0),
        ]

        self.run_stats_calc('ModelGeoStats', json.dumps(records))
        stats = self.get_stats_from_mmap(self.tmp_file_path('model_stats.mmap'), 'ModelStats')
        check_stats(stats, 120, OfferFlags.ADULT.value, 1800)
        check_stats(stats, 125, OfferFlags.ADULT.value, 1600)
        check_stats(stats, 130, 0, 1600)
