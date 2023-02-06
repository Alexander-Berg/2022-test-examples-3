# -*- coding: utf-8 -*-

from StringIO import StringIO
import os
import shutil
import unittest
from market.pylibrary.mindexerlib import util
from market.pylibrary.mbostuff import mbomodels
from market.pylibrary.mbostuff.utils.mbo_proto_write import MboOutputStream

from market.pylibrary.yatestwrap.yatestwrap import source_path
from getter import external
from getter import validator
from getter.exceptions import VerificationError

from market.proto.content.mbo.ExportReportModel_pb2 import ExportReportModel, LocalizedString, ParameterValue


def get_test_data_file_path(file_name):
    return source_path('market/getter/tests/data/{}'.format(file_name))


class Test(unittest.TestCase):
    def setUp(self):
        self.test_mbo_models_path = 'test_mbo_models'
        util.makedirs(self.test_mbo_models_path)

        model_with_bad_title = ExportReportModel(
            id=1,
            category_id=2,
            current_type='GURU',
            titles=[
                LocalizedString(isoCode='ru', value='new\nline')
            ],
            parameter_values=[
                ParameterValue(xsl_name='aliases', str_value=[LocalizedString(isoCode='ru', value='good alias')])
            ]
        )
        self.model_with_bad_title_path = os.path.join(self.test_mbo_models_path, 'model_with_bad_title')
        mbo_stream = MboOutputStream(self.model_with_bad_title_path, "MBEM")
        mbo_stream.write(model_with_bad_title.SerializeToString())

        model_with_bad_alias = ExportReportModel(
            id=2,
            category_id=2,
            current_type='GURU',
            titles=[
                LocalizedString(isoCode='ru', value='good title')
            ],
            parameter_values=[
                ParameterValue(xsl_name='aliases', str_value=[LocalizedString(isoCode='ru', value='new\nline')])
            ]
        )
        self.model_with_bad_alias_path = os.path.join(self.test_mbo_models_path, 'model_with_bad_alias')
        mbo_stream = MboOutputStream(self.model_with_bad_alias_path, "MBEM")
        mbo_stream.write(model_with_bad_alias.SerializeToString())

        good_model = ExportReportModel(
            id=3,
            category_id=2,
            current_type='GURU',
            titles=[
                LocalizedString(isoCode='ru', value='good title')
            ],
            parameter_values=[
                ParameterValue(xsl_name='aliases', str_value=[LocalizedString(isoCode='ru', value='good alias')])
            ]
        )
        self.good_model_path = os.path.join(self.test_mbo_models_path, 'good_model')
        mbo_stream = MboOutputStream(self.good_model_path, "MBEM")
        mbo_stream.write(good_model.SerializeToString())

    def tearDown(self):
        shutil.rmtree(self.test_mbo_models_path, ignore_errors=True)

    def test(self):
        validator.validate_csv(StringIO('1:2:3:4.34\n11:22:33:44.84\n'),
                               delimiter=':',
                               types=[int,
                                      int,
                                      validator.uint64,
                                      validator.ufloat])

    def test_buker(self):
        check = validator.validate_buker_file
        error = validator.VerificationError

        self.assertEquals(check(StringIO(''), delimiter=':'), None)
        self.assertRaises(error, check, StringIO(''), delimiter=':', minrecords=1)

        # валидный XML
        self.assertEquals(check(StringIO('a:<b></b>'), delimiter=':', minrecords=1), None)
        self.assertRaises(error, check, StringIO('a:<b><qqq></qqq>'), delimiter=':', minrecords=1)
        self.assertRaises(error, check, StringIO('a:<b><qqq></qqq><p k="'), delimiter=':', minrecords=1)

        # количество записей
        self.assertRaises(error, check, StringIO('a:<b></b>'), delimiter=':', minrecords=2)

    def test_mbo_models(self):
        validator.validate_mbo_models(self.good_model_path)  # do not raise ValueError

    def test_mbo_model_with_bad_title(self):
        validator.validate_mbo_models(self.model_with_bad_title_path)  # do not raise ValueError
        for model in mbomodels.mbo_models_reader(self.model_with_bad_title_path):
            for title in model.proto.titles:
                self.assertFalse('\n' in title.value)

    def test_mbo_model_with_bad_alias(self):
        validator.validate_mbo_models(self.model_with_bad_alias_path)  # do not raise ValueError
        for model in mbomodels.mbo_models_reader(self.model_with_bad_alias_path):
            for pv in model.proto.parameter_values:
                if pv.xsl_name == 'aliases':
                    self.assertFalse('\n' in pv.str_value)

    def test_validate_supplier_category_fees_xml(self):
        data = StringIO('<supplierCategoryFees><fee hyper_id="90401" supplier_id="" value="300"/></supplierCategoryFees>')
        validator.validate_supplier_category_fees_xml(data)

        data = StringIO('<supplierCategoryFees><fee hyper_id="90401" supplier_id="" value="11000"/></supplierCategoryFees>')
        with self.assertRaises(ValueError):
            validator.validate_supplier_category_fees_xml(data)

    def test_cpa_good(self):
        data = StringIO('''\
<?xml version='1.0' encoding='UTF-8'?>
<cpa-categories>
<category hyper_id="90401" fee="200" regions="225" cpa-type="cpc_and_cpa"/>
<category hyper_id="90586" fee="200" regions="213" cpa-type="hybrid_cpa_only"/>
<category hyper_id="91259" fee="0" regions="213" cpa-type="hybrid_cpa_only"/>
<category hyper_id="91517" fee="200" regions="213" cpa-type="hybrid_cpa_only"/>
<category hyper_id="91524" fee="200" regions="" cpa-type="hybrid_cpa_only"/>
<category hyper_id="91525" fee="200" regions="213" cpa-type="hybrid_cpa_only"/>
<category hyper_id="91540" fee="200" regions="213" cpa-type="hybrid_cpa_only"/>
<category hyper_id="91577" fee="200" regions="213" cpa-type="hybrid_cpa_only"/>
<category hyper_id="91579" fee="200" regions="213,114775" cpa-type="hybrid_cpa_only"/>
<category hyper_id="91708" fee="200" regions="213" cpa-type="hybrid_cpa_only"/>
<category hyper_id="226665" fee="100" regions="213" cpa-type="hybrid_cpa_only"/>
<category hyper_id="242699" fee="200" regions="213" cpa-type="hybrid_cpa_only"/>
<category hyper_id="277646" fee="400" regions="213" cpa-type="hybrid_cpa_only"/>
<category hyper_id="278345" fee="200" regions="213" cpa-type="hybrid_cpa_only"/>
<category hyper_id="431294" fee="800" regions="213" cpa-type="hybrid_cpa_only"/>
<category hyper_id="987827" fee="200" regions="213" cpa-type="hybrid_cpa_only"/>
<category hyper_id="4954975" fee="200" regions="213" cpa-type="cpa_only"/>
<category hyper_id="5017483" fee="200" regions="213" cpa-type="hybrid_cpa_only"/>
<category hyper_id="7070735" fee="200" regions="213" cpa-type="hybrid_cpa_only"/>
<category hyper_id="10498025" fee="200" regions="213" cpa-type="cpa_only"/>
<category hyper_id="10498025" fee="200" regions="213" cpa-type="cpa_with_cpc_pessimization"/>
<category hyper_id="10498025" fee="200" regions="213" cpa-type="cpa_non_guru"/>
<category hyper_id="10498025" fee="200" regions="213" cpa-type="cpa_non_guru" phone_rate="0" phone_threshold="0"/>
<category hyper_id="10498025" fee="200" regions="213" cpa-type="cpa_non_guru" phone_rate="10000" phone_threshold="12345"/>
</cpa-categories>''')
        validator.validate_cpa_categories(data)

    def run_cpa_bad_file(self, filepath):
        with self.assertRaises(VerificationError):
            validator.validate_cpa_categories(filepath)

    def test_cpa_bad_format(self):
        data = StringIO('''\
<category hyper_id="90401" fee="200" regions="225" cpa-type="cpc_and_cpa"/>
<category hyper_id="90586" fee="200" regions="213" cpa-type="hybrid_cpa_only"/>
<category hyper_id="91259" fee="200" regions="213" cpa-type="hybrid_cpa_only"/>
<category hyper_id="91517" fee="200" regions="213" cpa-type="hybrid_cpa_only"/>
<category hyper_id="91524" fee="200" regions="213" cpa-type="hybrid_cpa_only"/>
<category hyper_id="91525" fee="200" regions="213" cpa-type="hybrid_cpa_only"/>
<category hyper_id="91540" fee="200" regions="213" cpa-type="hybrid_cpa_only"/>
<category hyper_id="91577" fee="200" regions="213" cpa-type="hybrid_cpa_only"/>
<category hyper_id="91579" fee="200" regions="213" cpa-type="hybrid_cpa_only"/>
<category hyper_id="91708" fee="200" regions="213" cpa-type="hybrid_cpa_only"/>
<category hyper_id="226665" fee="200" regions="213" cpa-type="hybrid_cpa_only"/>
<category hyper_id="242699" fee="200" regions="213" cpa-type="hybrid_cpa_only"/>
<category hyper_id="277646" fee="200" regions="213" cpa-type="hybrid_cpa_only"/>
<category hyper_id="278345" fee="200" regions="213" cpa-type="hybrid_cpa_only"/>
<category hyper_id="431294" fee="200" regions="213" cpa-type="hybrid_cpa_only"/>
<category hyper_id="987827" fee="200" regions="213" cpa-type="hybrid_cpa_only"/>
<category hyper_id="4954975" fee="200" regions="213" cpa-type="cpa_only"/>
<category hyper_id="5017483" fee="200" regions="213" cpa-type="hybrid_cpa_only"/>
<category hyper_id="7070735" fee="200" regions="213" cpa-type="hybrid_cpa_only"/>
<category hyper_id="10498025" fee="200" regions="213" cpa-type="cpa_only"/>
</cpa-categories>''')
        self.run_cpa_bad_file(data)

    def test_cpa_bad_fee_1(self):
        data = StringIO('''\
<?xml version="1.0" encoding="UTF-8"?>
<cpa-categories>
 <category hyper_id="90401" fee="-1" regions="225" cpa-type="cpc_and_cpa"/>
</cpa-categories>''')
        self.run_cpa_bad_file(data)

    def test_cpa_bad_fee_2(self):
        data = StringIO('''
<?xml version="1.0" encoding="UTF-8"?>
<cpa-categories>
<category hyper_id="90401" fee="10001" regions="225" cpa-type="cpc_and_cpa"/>
</cpa-categories>''')
        self.run_cpa_bad_file(data)

    def test_cpa_bad_phone_1(self):
        data = StringIO('''
<category hyper_id="10498025" fee="200" regions="213" cpa-type="cpa_non_guru" phone_rate="-1" phone_threshold="0"/>
</cpa-categories>''')
        self.run_cpa_bad_file(data)

    def test_cpa_bad_phone_2(self):
        data = StringIO('''
<category hyper_id="10498025" fee="200" regions="213" cpa-type="cpa_non_guru" phone_rate="0" phone_threshold="-1"/>
</cpa-categories>''')
        self.run_cpa_bad_file(data)

    def test_cpa_bad_phone_3(self):
        data = StringIO('''
<category hyper_id="10498025" fee="200" regions="213" cpa-type="cpa_non_guru" phone_rate="10001" phone_threshold="0"/>
</cpa-categories>''')
        self.run_cpa_bad_file(data)

    def test_cpa_bad_phone_4(self):
        data = StringIO('''
<category hyper_id="10498025" fee="200" regions="213" cpa-type="cpa_non_guru" phone_rate="" phone_threshold="0"/>
</cpa-categories>''')
        self.run_cpa_bad_file(data)

    def test_cpa_bad_phone_5(self):
        data = StringIO('''
<category hyper_id="10498025" fee="200" regions="213" cpa-type="cpa_non_guru" phone_rate="0" phone_threshold="a"/>
</cpa-categories>''')
        self.run_cpa_bad_file(data)

    def test_validate_csv_good(self):
        data = StringIO('1:2:3.4\n1:2:3:4:5:6\n1:2:3.4:abc:def')
        validator.validate_csv(data, delimiter=':', types=[int, int, float])

    def test_validate_csv_bad_1(self):
        data = StringIO('1:2:abc')
        with self.assertRaises(VerificationError):
            validator.validate_csv(data, delimiter=':', types=[int, int, int])

    def test_validate_csv_bad_2(self):
        data = StringIO('1:2:3\n4:5\n6:7:8')
        with self.assertRaises(VerificationError):
            validator.validate_csv(data, delimiter=':', types=[int, int, int])

    def test_validate_csv_maybe_good(self):
        data = StringIO('1:2:3\n1::3\n4:5:6.7')
        validator.validate_csv(data, delimiter=':', types=[int, validator.maybe(int), float])

    def test_validate_csv_maybe_bad(self):
        data = StringIO('1:2:3\n1:2.3:3\n4:5:6.7')
        with self.assertRaises(VerificationError):
            validator.validate_csv(data, delimiter=':', types=[int, validator.maybe(int), float])

    def test_validate_csv_custom_handler(self):
        class ErrorHandler(object):
            def __init__(self, threshold):
                self.error_count = 0
                self.error_threshold = threshold

            def error(self, msg):
                self.error_count += 1

            def finish(self, nrows):
                error_rate = float(self.error_count) / float(nrows)
                if error_rate > self.error_threshold:
                    raise VerificationError('Error rate exceeds threshold!')

            def reset(self):
                self.error_count = 0

        handler = ErrorHandler(0.5)
        good_data = StringIO('ab:2\n4:5:6\n6:7:8')  # 1 bad line of total 3 - OK for our handler
        validator.validate_csv(good_data, delimiter=':', types=[int, int, int], error_handler=handler)

        bad_data = StringIO('1:2\n3:4\n5:6:7')  # 2 bad lines - handler raises an exception
        handler.reset()
        with self.assertRaises(VerificationError):
            validator.validate_csv(bad_data, delimiter=':', types=[int, int, int], error_handler=handler)

    def test_call_external_validator(self):
        true_tool = external.ExternalTool('/bin/true')
        false_tool = external.ExternalTool('/bin/false')
        bogus_tool = external.ExternalTool('/bogus')

        self.assertTrue(validator.call_external_validator(true_tool) is None)
        self.assertTrue(validator.call_external_validator(true_tool, '1') is None)
        self.assertTrue(validator.call_external_validator(bogus_tool) is None)

        from subprocess import CalledProcessError
        self.assertRaises(CalledProcessError, validator.call_external_validator, false_tool, '1')

    def test_validate_recommendation_data_file(self):
        data = StringIO('''\
model_id\treason_json
1000088\t[{"type": "statFactor", "id": "viewed_n_times", "value": 176.0}]
1000103\t[{"factor_id": "2004", "type": "consumerFactor", "id": "best_by_factor", "value": 0.5843023061752319}, {"type": "statFactor", "id": "viewed_n_times", "value": 139.0}]''')
        validator.validate_recommendations_data_file(data)

        # invalid number of fields
        bad_data = StringIO('''\
model_id\treason_json
123\tsss\tdd''')
        with self.assertRaises(VerificationError):
            validator.validate_recommendations_data_file(bad_data)

    def assert_is_correct_data(self, checker, data_path, **kwds):
        try:
            checker(get_test_data_file_path(data_path), **kwds)
            self.assertTrue(True)
        except VerificationError:
            self.assertFalse(True, "Exception should not be thrown for correct data!")

    def assert_is_invalid_data(self, checker, test_data, **kwds):
        try:
            checker(get_test_data_file_path(test_data), **kwds)
            self.assertFalse(True, "Exception should be thrown!")
        except VerificationError:
            self.assertTrue(True)

    def test_split_feed(self):
        try:
            validator.validate_shopsdat(get_test_data_file_path('shop_split_feed.txt'))
            self.assertFalse(True, "Exception should be thrown!")
        except Exception as e:
            self.assertTrue('Bad feed after line 21' in e.message)

    def test_validate_shop_rating_data_correct(self):
        self.assert_is_correct_data(validator.validate_shop_rating_data, "shop_rating_correct_data.txt")

    def test_validate_shop_rating_data_correct_minus_one(self):
        self.assert_is_correct_data(validator.validate_shop_rating_data, "shop_rating_correct_data_minus_one.txt")

    def test_validate_shop_rating_data_correct_new_data(self):
        self.assert_is_correct_data(validator.validate_shop_rating_data, "shop_rating_correct_data_new_format.txt")

    def test_validate_shop_rating_data_correct_new_data_minus_one(self):
        self.assert_is_correct_data(validator.validate_shop_rating_data, "shop_rating_correct_data_new_format_minus_one.txt")

    def test_validate_shop_rating_data_value_type_mismatch(self):
        self.assert_is_invalid_data(validator.validate_shop_rating_data, "shop_rating_value_type_mismatch.txt")

    def test_validate_shop_rating_data_empty(self):
        self.assert_is_invalid_data(validator.validate_shop_rating_data, "shop_rating_empty.txt")

    def test_validate_shop_rating_new_incorrect_rating(self):
        self.assert_is_invalid_data(validator.validate_shop_rating_data, "shop_rating_new_incorrect_rating.txt")

    def test_validate_shop_rating_new_incorrect_type_of_skk_disabled(self):
        self.assert_is_invalid_data(validator.validate_shop_rating_data, "shop_rating_new_incorrect_type_of_skk_disabled.txt")

    def test_validate_shop_rating_new_missing_mandatory_new_rating(self):
        self.assert_is_invalid_data(validator.validate_shop_rating_data, "shop_rating_missing_mandatory_new_rating.txt")

    def test_validate_shop_rating_new_missing_optional_shop_disabled(self):
        self.assert_is_correct_data(validator.validate_shop_rating_data, "shop_rating_missing_optional_shop_disabled.txt")

    def test_validate_shops_dat_warehouse_ok(self):
        self.assert_is_correct_data(validator.validate_blue_suppliers, "shop_warehouse_ok.txt")

    def test_validate_shops_dat_warehouse_bad(self):
        self.assert_is_invalid_data(validator.validate_blue_suppliers, "shop_warehouse_bad.txt")

    def test_validate_shops_dat_no_blue(self):
        self.assert_is_invalid_data(validator.validate_blue_suppliers, "shop_no_blue.txt")

    def test_validate_shops_dat_many_disables(self):
        self.assert_is_invalid_data(validator.validate_blue_suppliers, "shop_many_disabled.txt")

    def test_validate_shops_dat_many_disables_50(self):
        self.assert_is_invalid_data(validator.validate_blue_suppliers, "shop_many_disabled.txt", max_disabled_percentage=50.0)

    def test_validate_shops_dat_many_disables_70(self):
        self.assert_is_correct_data(validator.validate_blue_suppliers, "shop_many_disabled.txt", max_disabled_percentage=70.0)

    def test_validate_shops_dat_virtual_shops_ok(self):
        self.assert_is_correct_data(validator.validate_shopsdat, "virtual_shops_ok.txt")

    def test_validate_shops_dat_two_virtual_blue_shops(self):
        self.assert_is_invalid_data(validator.validate_shopsdat, "two_virtual_blue_shops.txt")

    def test_validate_shops_dat_two_virtual_red_shops(self):
        self.assert_is_invalid_data(validator.validate_shopsdat, "two_virtual_red_shops.txt")

    def test_validate_shops_dat_missing_virtual_blue_shop(self):
        self.assert_is_invalid_data(validator.validate_shopsdat, "missing_virtual_blue_shop.txt")

    def test_validate_shops_bad_vertical_field(self):
        self.assert_is_invalid_data(validator.validate_shopsdat, "shopsdat_bad_vertical_flag.txt")

    def test_validate_shops_ok_vertical_field(self):
        self.assert_is_correct_data(validator.validate_shopsdat, "shopsdat_ok_vertical_flag.txt")

if __name__ == '__main__':
    unittest.main()
