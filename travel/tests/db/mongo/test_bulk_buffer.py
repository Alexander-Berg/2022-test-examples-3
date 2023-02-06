# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import mock
import pytest
from hamcrest import has_entries, assert_that, contains_inanyorder

from common.db.mongo.bulk_buffer import BulkBuffer
from common.tester.utils.mongo import tmp_collection


class TestBulKBuffer(object):
    def test_valid(self):
        def check_items(*items):
            assert_that(coll.find(), contains_inanyorder(
                *(has_entries(item) for item in items)
            ))

        with tmp_collection('buffer_test') as coll:
            coll.update_one({'key': 1}, {'$set': {'value': 1}}, upsert=True)
            check_items({'key': 1, 'value': 1})

            with BulkBuffer(coll, max_buffer_size=3) as coll_buff:
                coll_buff.update_one({'key': 1}, {'$set': {'value': 11}}, upsert=True)
                coll_buff.update_one({'key': 2}, {'$set': {'value': 2}}, upsert=True)
                check_items({'key': 1, 'value': 1})

                coll_buff.update_one({'key': 3}, {'$set': {'value': 3}}, upsert=True)
                check_items({'key': 1, 'value': 11}, {'key': 2, 'value': 2}, {'key': 3, 'value': 3})

                coll_buff.update_one({'key': 3}, {'$set': {'value': 33}}, upsert=True)
                check_items({'key': 1, 'value': 11}, {'key': 2, 'value': 2}, {'key': 3, 'value': 3})

                coll_buff.insert_one({'key': 4, 'value': 44})
                check_items({'key': 1, 'value': 11}, {'key': 2, 'value': 2}, {'key': 3, 'value': 3})

                coll_buff.insert_one({'key': 4, 'value': 444})
                check_items(
                    {'key': 1, 'value': 11}, {'key': 2, 'value': 2}, {'key': 3, 'value': 33},
                    {'key': 4, 'value': 44}, {'key': 4, 'value': 444}
                )

                coll_buff.delete_many({'key': 4})
                check_items(
                    {'key': 1, 'value': 11}, {'key': 2, 'value': 2}, {'key': 3, 'value': 33},
                    {'key': 4, 'value': 44}, {'key': 4, 'value': 444}
                )

                coll_buff.delete_one({'key': 1})
                check_items(
                    {'key': 1, 'value': 11}, {'key': 2, 'value': 2}, {'key': 3, 'value': 33},
                    {'key': 4, 'value': 44}, {'key': 4, 'value': 444}
                )

            check_items({'key': 2, 'value': 2}, {'key': 3, 'value': 33})
            assert coll_buff.operations_processed == 8

    def test_invalid_operation(self):
        with tmp_collection('buffer_test') as coll:
            with BulkBuffer(coll, max_buffer_size=3) as coll_buff:
                with pytest.raises(AttributeError):
                    coll_buff.insert123()

    def test_on_before_flush(self):
        with tmp_collection('buffer_test') as coll:
            on_before_flush = mock.Mock()

            with BulkBuffer(coll, max_buffer_size=2, on_before_flush=on_before_flush) as coll_buff:
                coll_buff.update_one({'key': 1}, {'$set': {'value': 11}}, upsert=True)

                assert len(on_before_flush.call_args_list) == 0

                coll_buff.insert_one({'key': 4, 'value': 444})
                assert len(on_before_flush.call_args_list) == 1
                assert on_before_flush.call_args_list[0][0][0] == coll_buff

                coll_buff.delete_one({'key': 1})
                assert len(on_before_flush.call_args_list) == 1

            assert len(on_before_flush.call_args_list) == 2
            assert on_before_flush.call_args_list[1][0][0] == coll_buff
            assert coll_buff.operations_processed == 3
