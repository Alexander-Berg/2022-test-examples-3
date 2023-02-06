#!/usr/bin/env python
# -*- coding: utf-8 -*-

import subprocess
import unittest
import yatest

from mapreduce.yt.python.yt_stuff import YtStuff, YtConfig
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from yt.wrapper import ypath_join

SHOP_OUTLET_XML_INVALID = yatest.common.source_path("market/tools/getter_validators/shops-outlet-xml2mmap-converter/ut/shops_outlet_invalid.xml")
SHOP_OUTLET_XML_VALID = yatest.common.source_path("market/tools/getter_validators/shops-outlet-xml2mmap-converter/ut/shops_outlet_valid.xml")
SHOP_OUTLET_V7_MMAP = yatest.common.output_path("shops_outlet.v7.mmap")
XML2MMAP_CONVERTER = yatest.common.binary_path("market/tools/getter_validators/shops-outlet-xml2mmap-converter/shops-outlet-xml2mmap-converter")

# Valid outlets that have all required fields.
YT_OUTLETS_DATA_VALID = [
    {
        'lms_id': 10000000100,
        'delivery_service_id': 104,
        'delivery_service_outlet_code': 'Ab124C',
        'is_active': True,
        'working_time': [
            {
                '1': {
                    'from': '10:00:00',
                    'to': '20:00:00'
                }
            },
            {
                '5': {
                    'from': '12:00:00',
                    'to': '21:00:00'
                }
            },
        ],
        'telephone': {
            'cityCode': '495',
            'countryCode': '7',
            'telephoneNumber': '9843122',
            'telephoneType': 'PHONE'
        },
        'gps_coord': '56.298342,43.938038',
        'email': 'outletHamovniki@delivery.com',
        'mbi_id': 129300,
    },
    # This outlet has is_active=False so it is skipped according to the current logic.
    # Change validation logic in shops_outlet_mms to allow outlets that have is_active=False
    {
        'lms_id': 10000000101,
        'delivery_service_id': 104,
        'delivery_service_outlet_code': 'Ab124C',
        'is_active': False,
        'working_time': [
            {
                '1': {
                    'from': '10:00:00',
                    'to': '20:00:00'
                }
            },
            {
                '5': {
                    'from': '12:00:00',
                    'to': '21:00:00'
                }
            },
        ],
        'telephone': {
            'cityCode': '495',
            'countryCode': '7',
            'telephoneNumber': '9843122',
            'telephoneType': 'PHONE'
        },
        'gps_coord': '56.298342,43.938038',
        'email': 'outletHamovniki@delivery.com',
        'mbi_id': 129300,
    },
]

# Invalid outlets that have at least one required field missing.
# They will be skipped and won't be written to shops_outlet.mmap
YT_OUTLETS_DATA_MISSING_FIELDS = [
    {
        # missing lms_id
        'delivery_service_id': 103,
        'mbi_id': 129300,
        'email': 'outletHamovniki@delivery.com',
        'is_active': True,
    },
    {
        # missing delivery_service_id
        'lms_id': 10000000200,
        'mbi_id': 129300,
        'email': 'outletHamovniki@delivery.com',
        'is_active': True,
    },
    {
        # missing delivery_service_outlet_code
        'lms_id': 10000000201,
        'delivery_service_id': 103,
        'email': 'outletHamovniki@delivery.com',
    },
    {
        # missing is_active
        'lms_id': 10000000202,
        'delivery_service_id': 104,
        'delivery_service_outlet_code': 'Ab124C',
        'email': 'outletHamovniki@delivery.com',
    },
    {
        # missing working_time
        'lms_id': 10000000203,
        'delivery_service_id': 105,
        'delivery_service_outlet_code': 'Ab125C',
        'is_active': True,
        'email': 'outletHamovniki@delivery.com',
    },
    {
        # missing telephone
        'lms_id': 10000000204,
        'delivery_service_id': 105,
        'delivery_service_outlet_code': 'Ab125C',
        'is_active': True,
        'working_time': [
            {
                '1': {
                    'from': '10:00:00',
                    'to': '20:00:00'
                }
            },
            {
                '5': {
                    'from': '12:00:00',
                    'to': '21:00:00'
                }
            },
        ],
        'email': 'outletHamovniki@delivery.com',
    },
    {
        # missing gps_coord
        'lms_id': 10000000205,
        'delivery_service_id': 105,
        'delivery_service_outlet_code': 'Ab125C',
        'is_active': True,
        'working_time': [
            {
                '1': {
                    'from': '10:00:00',
                    'to': '20:00:00'
                }
            },
        ],
        'telephone': {
            'cityCode': '495',
            'countryCode': '7',
            'telephoneNumber': '9843122',
            'telephoneType': 'PHONE'
        },
        'email': 'outletHamovniki@delivery.com',
    },
]


# Invalid outlets that have type mismathch for at least one required field.
# They will be skipped and won't be written to shops_outlet.mmap
YT_OUTLETS_DATA_MISMATCH_TYPE = [
    {
        # type mismatch for lms_id
        'lms_id': None,
        'delivery_service_id': 105,
        'delivery_service_outlet_code': 'Ab125C',
        'is_active': True,
        'working_time': [
            {
                '1': {
                    'from': '10:00:00',
                    'to': '20:00:00'
                }
            },
        ],
        'telephone': {
            'cityCode': '495',
            'countryCode': '7',
            'telephoneNumber': '9843122',
            'telephoneType': 'PHONE'
        },
        'email': 'outletHamovniki@delivery.com',
    },
    {
        # type mismatch for delivery_service_id
        'lms_id': 10000000206,
        'delivery_service_id': None,
        'delivery_service_outlet_code': 'Ab125C',
        'is_active': True,
        'working_time': [
            {
                '1': {
                    'from': '10:00:00',
                    'to': '20:00:00'
                }
            },
        ],
        'telephone': {
            'cityCode': '495',
            'countryCode': '7',
            'telephoneNumber': '9843122',
            'telephoneType': 'PHONE'
        },
        'email': 'outletHamovniki@delivery.com',
    },
    {
        # type mismatch for delivery_service_outlet_code
        'lms_id': 10000000207,
        'delivery_service_id': 105,
        'delivery_service_outlet_code': None,
        'is_active': True,
        'working_time': [
            {
                '1': {
                    'from': '10:00:00',
                    'to': '20:00:00'
                }
            },
        ],
        'telephone': {
            'cityCode': '495',
            'countryCode': '7',
            'telephoneNumber': '9843122',
            'telephoneType': 'PHONE'
        },
        'email': 'outletHamovniki@delivery.com',
    },
    {
        # type mismatch for is_active
        'lms_id': 10000000208,
        'delivery_service_id': 105,
        'delivery_service_outlet_code': 'Ab125C',
        'is_active': None,
        'working_time': [
            {
                '1': {
                    'from': '10:00:00',
                    'to': '20:00:00'
                }
            },
        ],
        'telephone': {
            'cityCode': '495',
            'countryCode': '7',
            'telephoneNumber': '9843122',
            'telephoneType': 'PHONE'
        },
        'email': 'outletHamovniki@delivery.com',
    },
    {
        # type mismatch for working_time
        'lms_id': 10000000209,
        'delivery_service_id': 105,
        'delivery_service_outlet_code': 'Ab125C',
        'is_active': True,
        'working_time': None,
        'telephone': {
            'cityCode': '495',
            'countryCode': '7',
            'telephoneNumber': '9843122',
            'telephoneType': 'PHONE'
        },
        'email': 'outletHamovniki@delivery.com',
    },
    {
        # type mismatch for telephone
        'lms_id': 10000000210,
        'delivery_service_id': 105,
        'delivery_service_outlet_code': 'Ab125C',
        'is_active': True,
        'working_time': [
            {
                '1': {
                    'from': '10:00:00',
                    'to': '20:00:00'
                }
            },
        ],
        'telephone': None,
        'email': 'outletHamovniki@delivery.com',
    },
    {
        # type mismatch for gps_coord
        'lms_id': 10000000211,
        'delivery_service_id': 105,
        'delivery_service_outlet_code': 'Ab125C',
        'is_active': True,
        'working_time': [
            {
                '1': {
                    'from': '10:00:00',
                    'to': '20:00:00'
                }
            },
        ],
        'telephone': {
            'cityCode': '495',
            'countryCode': '7',
            'telephoneNumber': '9843122',
            'telephoneType': 'PHONE'
        },
        'gps_coord': None,
        'email': 'outletHamovniki@delivery.com',
    },
]

YT_OUTLETS_DATA_CONTENT_ERRORS = [
    {
        # working_time is empty list
        'lms_id': 10000000212,
        'delivery_service_id': 105,
        'delivery_service_outlet_code': 'Ab125C',
        'is_active': True,
        'working_time': [],
        'telephone': {
            'cityCode': '495',
            'countryCode': '7',
            'telephoneNumber': '9843122',
            'telephoneType': 'PHONE'
        },
        'gps_coord': None,
        'email': 'outletHamovniki@delivery.com',
    },
    {
        # hours_from is empty string
        'lms_id': 10000000213,
        'delivery_service_id': 105,
        'delivery_service_outlet_code': 'Ab125C',
        'is_active': True,
        'working_time': [
            {
                '1': {
                    'from': '',
                    'to': '20:00:00'
                }
            },
        ],
        'telephone': {
            'cityCode': '495',
            'countryCode': '7',
            'telephoneNumber': '9843122',
            'telephoneType': 'PHONE'
        },
        'gps_coord': None,
        'email': 'outletHamovniki@delivery.com',
    },
    {
        # hours_to is empty string
        'lms_id': 10000000214,
        'delivery_service_id': 105,
        'delivery_service_outlet_code': 'Ab125C',
        'is_active': True,
        'working_time': [
            {
                '1': {
                    'from': '10:00:00',
                    'to': ''
                }
            },
        ],
        'telephone': {
            'cityCode': '495',
            'countryCode': '7',
            'telephoneNumber': '9843122',
            'telephoneType': 'PHONE'
        },
        'gps_coord': None,
        'email': 'outletHamovniki@delivery.com',
    },
]

YT_OUTLETS_DATA_INVALID = YT_OUTLETS_DATA_MISSING_FIELDS + YT_OUTLETS_DATA_MISMATCH_TYPE + YT_OUTLETS_DATA_CONTENT_ERRORS
YT_OUTLETS_DATA = YT_OUTLETS_DATA_VALID + YT_OUTLETS_DATA_INVALID


class T(unittest.TestCase):
    maxDiff = None

    def test_delivery_services_generator(self):
        cmd_list = [
            XML2MMAP_CONVERTER,
            "--xml", SHOP_OUTLET_XML_INVALID,
            "--mmap", SHOP_OUTLET_V7_MMAP,
            "--mode", "xml",
        ]
        proc = subprocess.Popen(args=cmd_list, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
        assert proc.stdout is not None
        output = proc.stdout.read()
        target_str = "Gps coords are not specified for outlet 288899"
        assert target_str in output

    def test_read_from_xml_and_yt_and_write_to_mmap(self):
        yt_server = YtStuff(YtConfig(wait_tablet_cell_initialization=True))
        yt_server.start_local_yt()
        yt_client = yt_server.get_yt_client()

        YT_OUTLETS_TABLE_PATH = ypath_join(get_yt_prefix(), 'market', 'logistics_management_service', 'yt_outlet')
        YT_TOKEN_PATH = ''

        yt_client.create('table', YT_OUTLETS_TABLE_PATH, ignore_existing=True, recursive=True)
        yt_client.write_table(YT_OUTLETS_TABLE_PATH, YT_OUTLETS_DATA)

        cmd_list = [
            XML2MMAP_CONVERTER,
            '--xml', SHOP_OUTLET_XML_VALID,
            '--mmap', SHOP_OUTLET_V7_MMAP,
            '--yt-cluster', yt_server.get_server(),
            '--yt-table', YT_OUTLETS_TABLE_PATH,
            '--yt-token', YT_TOKEN_PATH,
            '--mode', 'xml-and-yt',
        ]

        proc = subprocess.Popen(args=cmd_list, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)

        assert proc.stdout is not None
        output = proc.stdout.read()
        # outlet_id=0 is the default outlet_id for cases when lms_id is absent in YT table.
        # TODO: Currently outlet_ids are checked in internal numeration.
        # If lms_id > 10'000'000'000 then subtract 8'000'000'000 offset to make sure it fits into 32 bits.
        # This will be fixed after 64-bit outlet_ids are supported in shops_outlet_mms. Then we won't need this hack.
        expected_items_missing_field = [
            (0, 'lms_id'),
            (10000000200, 'delivery_service_id'),
            (10000000201, 'delivery_service_outlet_code'),
            (10000000202, 'is_active'),
            (10000000203, 'working_time'),
            (10000000204, 'telephone'),
            (10000000205, 'gps_coord'),
        ]
        for outlet_id, missing_column in expected_items_missing_field:
            skip_str = 'OutletId {} was skipped.'.format(outlet_id)
            reason_str = 'Reason: mandatory column "{}" is absent in YT table.'.format(missing_column)
            target_str = skip_str + ' ' + reason_str
            assert target_str in output

        expected_items_mismatch_type = [
            (0, 'lms_id', 'Int64'),
            (10000000206, 'delivery_service_id', 'Int64'),
            (10000000207, 'delivery_service_outlet_code', 'String'),
            (10000000208, 'is_active', 'Bool'),
            (10000000209, 'working_time', 'List'),
            (10000000210, 'telephone', 'Map'),
            (10000000211, 'gps_coord', 'String'),
        ]
        for outlet_id, yt_column, expected_type in expected_items_mismatch_type:
            skip_str = 'OutletId {} was skipped.'.format(outlet_id)
            reason_str = 'Reason: invalid type for column "{}", expected type: {}.'.format(yt_column, expected_type)
            target_str = skip_str + ' ' + reason_str
            assert target_str in output

        expected_items_content_errors = [
            (10000000212, '"working_time" is empty list'),
            (10000000213, '"hours_from" is empty string'),
            (10000000214, '"hours_to" is empty string'),
        ]
        for outlet_id, reason in expected_items_content_errors:
            skip_str = 'OutletId {} was skipped.'.format(outlet_id)
            reason_str = 'Reason: {}.'.format(reason)
            target_str = skip_str + ' ' + reason_str
            assert target_str in output

        # Check that outlet with is_active=False was skipped
        skip_str = 'OutletId {} was skipped. Reason: '.format(10000000200)
        assert skip_str in output


if __name__ == '__main__':
    unittest.main()
