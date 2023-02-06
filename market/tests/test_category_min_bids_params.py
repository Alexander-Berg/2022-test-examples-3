# -*- coding: utf-8 -*-

from StringIO import StringIO
import unittest

from getter import category_min_bids_params


class Test(unittest.TestCase):
    def test_good(self):
        data = StringIO('''
        <categories>
            <category id="90401">
                <searchBidParams coefficient="0.245" power="0.56" maxBid="90"/>
                <cardBidParams coefficient="0.0399" power="0.6666666" maxBid="90"/>
            </category>
            <category id="90555">
                <cardBidParams coefficient="0.0399" power="0.6666666" maxBid="90"/>
            </category>
            <category id="10682671">
                <searchBidParams coefficient="0.245" power="0.56" maxBid="90"/>
            </category>
        </categories>
        ''')
        category_min_bids_params.validate(data)

    def test_no_root(self):
        data = StringIO('''
        <categories>
            <category id="90555">
                <cardBidParams coefficient="0.0399" power="0.6666666" maxBid="90"/>
            </category>
            <category id="10682671">
                <searchBidParams coefficient="0.245" power="0.56" maxBid="90"/>
            </category>
        </categories>
        ''')
        with self.assertRaises(category_min_bids_params.Error):
            category_min_bids_params.validate(data)

    def test_root_not_card_bid(self):
        data = StringIO('''
        <categories>
            <category id="90401">
                <searchBidParams coefficient="0.245" power="0.56" maxBid="90"/>
            </category>
            <category id="90555">
                <cardBidParams coefficient="0.0399" power="0.6666666" maxBid="90"/>
            </category>
            <category id="10682671">
                <searchBidParams coefficient="0.245" power="0.56" maxBid="90"/>
            </category>
        </categories>
        ''')
        with self.assertRaises(category_min_bids_params.Error):
            category_min_bids_params.validate(data)

    def test_root_not_search_bid(self):
        data = StringIO('''
        <categories>
            <category id="90401">
                <cardBidParams coefficient="0.0399" power="0.6666666" maxBid="90"/>
            </category>
            <category id="90555">
                <cardBidParams coefficient="0.0399" power="0.6666666" maxBid="90"/>
            </category>
            <category id="10682671">
                <searchBidParams coefficient="0.245" power="0.56" maxBid="90"/>
            </category>
        </categories>
        ''')
        with self.assertRaises(category_min_bids_params.Error):
            category_min_bids_params.validate(data)

    def test_coefficient_missed(self):
        data = StringIO('''
        <categories>
            <category id="90401">
                <searchBidParams coefficient="0.245" power="0.56" maxBid="90"/>
                <cardBidParams coefficient="0.0399" power="0.6666666" maxBid="90"/>
            </category>
            <category id="90555">
                <cardBidParams power="0.6666666" maxBid="90"/>
            </category>
            <category id="10682671">
                <searchBidParams coefficient="0.245" power="0.56" maxBid="90"/>
            </category>
        </categories>
        ''')
        with self.assertRaises(Exception):
            category_min_bids_params.validate(data)

    def test_power_missed(self):
        data = StringIO('''
        <categories>
            <category id="90401">
                <searchBidParams coefficient="0.245" power="0.56" maxBid="90"/>
                <cardBidParams coefficient="0.0399" power="0.6666666" maxBid="90"/>
            </category>
            <category id="90555">
                <cardBidParams coefficient="0.0399" maxBid="90"/>
            </category>
            <category id="10682671">
                <searchBidParams coefficient="0.245" power="0.56" maxBid="90"/>
            </category>
        </categories>
        ''')
        with self.assertRaises(Exception):
            category_min_bids_params.validate(data)

    def test_max_bid_missed(self):
        data = StringIO('''
        <categories>
            <category id="90401">
                <searchBidParams coefficient="0.245" power="0.56" maxBid="90"/>
                <cardBidParams coefficient="0.0399" power="0.6666666" maxBid="90"/>
            </category>
            <category id="90555">
                <cardBidParams coefficient="0.0399" power="0.6666666"/>
            </category>
            <category id="10682671">
                <searchBidParams coefficient="0.245" power="0.56" maxBid="90"/>
            </category>
        </categories>
        ''')
        with self.assertRaises(Exception):
            category_min_bids_params.validate(data)

    def test_coefficient_wrong_type(self):
        data = StringIO('''
        <categories>
            <category id="90401">
                <searchBidParams coefficient="0.245" power="0.56" maxBid="90"/>
                <cardBidParams coefficient="0.0399" power="0.6666666" maxBid="90"/>
            </category>
            <category id="90555">
                <cardBidParams coefficient="0.0399" power="0.6666666" maxBid="90"/>
            </category>
            <category id="10682671">
                <searchBidParams coefficient="not float" power="0.56" maxBid="90"/>
            </category>
        </categories>
        ''')
        with self.assertRaises(Exception):
            category_min_bids_params.validate(data)

    def test_power_wrong_type(self):
        data = StringIO('''
        <categories>
            <category id="90401">
                <searchBidParams coefficient="0.245" power="0.56" maxBid="90"/>
                <cardBidParams coefficient="0.0399" power="0.6666666" maxBid="90"/>
            </category>
            <category id="90555">
                <cardBidParams coefficient="0.0399" power="0.6666666" maxBid="90"/>
            </category>
            <category id="10682671">
                <searchBidParams coefficient="0.245" power="not float" maxBid="90"/>
            </category>
        </categories>
        ''')
        with self.assertRaises(Exception):
            category_min_bids_params.validate(data)

    def test_max_bid_wrong_type(self):
        data = StringIO('''
        <categories>
            <category id="90401">
                <searchBidParams coefficient="0.245" power="0.56" maxBid="90"/>
                <cardBidParams coefficient="0.0399" power="0.6666666" maxBid="90"/>
            </category>
            <category id="90555">
                <cardBidParams coefficient="0.0399" power="0.6666666" maxBid="90"/>
            </category>
            <category id="10682671">
                <searchBidParams coefficient="0.245" power="0.56" maxBid="not int"/>
            </category>
        </categories>
        ''')
        with self.assertRaises(Exception):
            category_min_bids_params.validate(data)


if __name__ == '__main__':
    unittest.main()
