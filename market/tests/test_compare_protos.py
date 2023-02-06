# -*- coding: utf-8 -*-
import market.idx.pylibrary.mindexer_core.monitoring_supercontroller.monitoring_supercontroller as monitoring_supercontroller
from library.python.protobuf.get_serialized_file_descriptor_set import get_file_descriptor_set
from market.idx.pylibrary.mindexer_core.monitoring_supercontroller.tests.proto.test_1 import test1_pb2
from market.idx.pylibrary.mindexer_core.monitoring_supercontroller.tests.proto.test_2 import test2_pb2
from market.idx.pylibrary.mindexer_core.monitoring_supercontroller.tests.proto.test_3 import test3_pb2
from hamcrest import assert_that


def test_compare_equal_messages():
    checker = monitoring_supercontroller.CheckProtoOfferFields(None, None, None, None)

    offers_data_set = get_file_descriptor_set(test1_pb2.Proto)
    checker.compare_file_descriptor_sets(offers_data_set, offers_data_set)
    assert_that(checker.result == monitoring_supercontroller._OK and checker.message == "")


def test_compare_different_fields():
    # only warn, because they have different names(like paths)
    checker = monitoring_supercontroller.CheckProtoOfferFields(None, None, None, None)

    offers_data_set_1 = get_file_descriptor_set(test1_pb2.Proto)
    offers_data_set_2 = get_file_descriptor_set(test2_pb2.Proto)
    checker.compare_file_descriptor_sets(offers_data_set_1, offers_data_set_2)
    assert_that(checker.result == monitoring_supercontroller._WARN and checker.message != "")


def test_compare_fields_with_different_attributes():
    # Ignore change in proto attributes
    checker = monitoring_supercontroller.CheckProtoOfferFields(None, None, None, None)

    offers_data_set_1 = get_file_descriptor_set(test1_pb2.Proto)
    offers_data_set_3 = get_file_descriptor_set(test3_pb2.Proto)
    offers_data_1 = [k for k in offers_data_set_1.file]
    offers_data_3 = [k for k in offers_data_set_3.file]

    checker.compare_file_descriptor_proto(offers_data_1[0], offers_data_3[0], '')
    assert checker.result == monitoring_supercontroller._OK
    assert checker.message == ''
