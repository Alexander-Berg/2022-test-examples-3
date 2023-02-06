# coding: utf-8

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from search.priemka.yappy.proto.structures.beta_component_pb2 import ComponentType

from search.priemka.yappy.src.model.lineage2_service import conversions

from search.priemka.yappy.tests.utils.test_cases import TestCase
from search.priemka.yappy.tests.utils.fill_message import fill_message


class Lineage2ConversionsTestCase(TestCase):

    def test_component_type_conversion_field_names(self):
        ctype = ComponentType()
        fill_message(ctype, ignore_fields=['etag', 'last_update'])

        api_ctype = conversions.convert_component_type(ctype)
        double_converted = conversions.convert_api_component_type(api_ctype)

        orig_fields = sorted([field.name for field, _ in ctype.ListFields()])
        converted_fields = sorted([field.name for field, _ in double_converted.ListFields()])

        self.assertEqual(orig_fields, converted_fields)
