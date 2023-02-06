import os
import src.util as util
import unittest

from src import ticket
from . import context


class TicketTest(unittest.TestCase):
    def setUp(self):
        ticket_file = os.path.join(context.ut_resources_dir, 'ticket.json')
        self.raw_ticket = util.read_json_file(ticket_file)

        converted_ticket_file = os.path.join(context.ut_resources_dir,
                                             'ticket.converted.json')
        self.converted_ticket = util.read_json_file(converted_ticket_file)

    def test_convert(self):
        actual = ticket.convert(self.raw_ticket)
        self.assertEqual(actual, self.converted_ticket)

    def test_good_order_id(self):
        actual = ticket.get_order_id(
            {'fields': [{
                'id': 360010510132,
                'value': '3829773'
            }]})
        self.assertEqual(actual, '3829773')

    def test_no_order_id(self):
        actual = ticket.get_order_id(
            {'fields': [{
                'id': 1234,
                'value': '3829773'
            }]})
        self.assertEqual(actual, None)

    def test_zero_order_id(self):
        actual = ticket.get_order_id(
            {'fields': [{
                'id': 360010510132,
                'value': '0'
            }]})
        self.assertEqual(actual, None)

    def test_order_id_in_other_field(self):
        actual = ticket.get_order_id(
            {'fields': [{
                'id': 360009692772,
                'value': '1234'
            }]})
        self.assertEqual(actual, '1234')

    def test_order_id_in_both_fields1(self):
        actual = ticket.get_order_id({
            'fields': [{
                'id': 360010510132,
                'value': '123'
            }, {
                'id': 360009692772,
                'value': '1234'
            }]
        })
        self.assertEqual(actual, '123')

    def test_order_id_in_both_fields2(self):
        actual = ticket.get_order_id({
            'fields': [{
                'id': 360010510132,
                'value': '0'
            }, {
                'id': 360009692772,
                'value': '1234'
            }]
        })
        self.assertEqual(actual, '1234')

    def test_channel_is_email(self):
        self.assertTrue(ticket.is_channel_email_or_api(self.raw_ticket))

    def test_channel_is_api(self):
        self.assertTrue(
            ticket.is_channel_email_or_api({'via': {
                'channel': 'api'
            }}))

    def test_channel_is_not_email_or_api(self):
        self.assertFalse(
            ticket.is_channel_email_or_api({'via': {
                'channel': 'web'
            }}))


if __name__ == '__main__':
    unittest.main()
