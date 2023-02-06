#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import GLParam, GLType, Model, Vendor
from core.testcase import TestCase, main
from core.matcher import NoKey, NotEmpty


class T(TestCase):
    """Test that fake vendor (special vendor with id 15728405) either hides from output or replaces with
    vendor hypothesis"""

    fake_vendor_id = 16644882
    raw_vendor_param_id = 16652618

    @classmethod
    def prepare_fake_vendors(cls):
        GLType(hid=2, param_id=cls.raw_vendor_param_id, name='raw vendor', xslname="raw_vendor", gltype=GLType.ENUM),

        cls.index.models += [
            Model(hyperid=1, title='workbench with fake vendor', hid=1, vendor_id=cls.fake_vendor_id),
            Model(
                hyperid=2,
                title='workbench with fake vendor and vendor hypothesis',
                hid=2,
                vendor_id=cls.fake_vendor_id,
                glparams=[GLParam(param_id=cls.raw_vendor_param_id, string_value='Abibas', is_hypothesis=True)],
            ),
            Model(
                hyperid=21,
                title='workbench with fake vendor and invalid vendor hypothesis',
                hid=2,
                vendor_id=cls.fake_vendor_id,
                glparams=[GLParam(param_id=cls.raw_vendor_param_id, string_value='', is_hypothesis=True)],
            ),
            Model(hyperid=3, title='workbench with normal vendor', hid=3, vendor_id=1),
        ]

        cls.index.vendors += [
            Vendor(vendor_id=1, name='not fake'),
            Vendor(vendor_id=cls.fake_vendor_id, name='fake af'),
        ]

    def test_hide_fake_vendors(self):
        """Check that 'vendor' field disappears on model with fake vendor_id"""
        request = 'place=prime&text=workbench'

        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            [
                {
                    'entity': 'product',
                    'id': 1,
                    'vendor': NoKey('vendor'),
                },
                {
                    'entity': 'product',
                    'id': 3,
                    'vendor': NotEmpty(),
                },
            ],
        )


if __name__ == '__main__':
    main()
