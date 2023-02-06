from datetime import date, datetime

from travel.hotels.tools.affiliate_data_builder.lib.commission_config import CommissionConfig


NOW = datetime(2021, 11, 15)
DATE_FROM = date(2021, 11, 14)


def get_read_table_method(table_data):
    def read_table_method(path):
        return table_data[path]
    return read_table_method


def test_read_config():
    config = CommissionConfig(None, DATE_FROM, NOW)
    table_data = {
        '/affiliate_partners': [
            {
                'AffiliatePartnerName': 'partner_1',
                'Category': 'category_1',
                'OrderType': ['order_type_1_1', 'order_type_1_2'],
            },
            {
                'AffiliatePartnerName': 'partner_2',
                'Category': 'category_2',
                'OrderType': ['order_type_2_1', 'order_type_2_2'],
            },
        ],
        '/affiliate_partner_commission': [
            {
                'AffiliatePartnerName': '*',
                'Category': '*',
                'OrderType': '*',
                'Commission': 1.1,
            },
        ],
        '/affiliate_user_commission': [
            {
                'AffiliatePartnerName': '*',
                'Category': '*',
                'OrderType': '*',
                'UserId': 'user_3',
                'Commission': 3.3,
            },
        ],
    }
    expected_data = {
        '/affiliate_partners': [
            {
                'Date': str(DATE_FROM),
                'AffiliatePartnerName': 'partner_1',
                'Category': 'category_1',
                'OrderType': 'order_type_1_1',
            },
            {
                'Date': str(DATE_FROM),
                'AffiliatePartnerName': 'partner_1',
                'Category': 'category_1',
                'OrderType': 'order_type_1_2',
            },
            {
                'Date': str(DATE_FROM),
                'AffiliatePartnerName': 'partner_2',
                'Category': 'category_2',
                'OrderType': 'order_type_2_1',
            },
            {
                'Date': str(DATE_FROM),
                'AffiliatePartnerName': 'partner_2',
                'Category': 'category_2',
                'OrderType': 'order_type_2_2',
            },
            {
                'Date': str(NOW.date()),
                'AffiliatePartnerName': 'partner_1',
                'Category': 'category_1',
                'OrderType': 'order_type_1_1',
            },
            {
                'Date': str(NOW.date()),
                'AffiliatePartnerName': 'partner_1',
                'Category': 'category_1',
                'OrderType': 'order_type_1_2',
            },
            {
                'Date': str(NOW.date()),
                'AffiliatePartnerName': 'partner_2',
                'Category': 'category_2',
                'OrderType': 'order_type_2_1',
            },
            {
                'Date': str(NOW.date()),
                'AffiliatePartnerName': 'partner_2',
                'Category': 'category_2',
                'OrderType': 'order_type_2_2',
            },
        ],
        '/affiliate_partner_commission': [
            {
                'Date': str(DATE_FROM),
                'AffiliatePartnerName': 'partner_1',
                'Category': 'category_1',
                'OrderType': 'order_type_1_1',
                'Commission': 1.1,
            },
            {
                'Date': str(DATE_FROM),
                'AffiliatePartnerName': 'partner_1',
                'Category': 'category_1',
                'OrderType': 'order_type_1_2',
                'Commission': 1.1,
            },
            {
                'Date': str(DATE_FROM),
                'AffiliatePartnerName': 'partner_2',
                'Category': 'category_2',
                'OrderType': 'order_type_2_1',
                'Commission': 1.1,
            },
            {
                'Date': str(DATE_FROM),
                'AffiliatePartnerName': 'partner_2',
                'Category': 'category_2',
                'OrderType': 'order_type_2_2',
                'Commission': 1.1,
            },
            {
                'Date': str(NOW.date()),
                'AffiliatePartnerName': 'partner_1',
                'Category': 'category_1',
                'OrderType': 'order_type_1_1',
                'Commission': 1.1,
            },
            {
                'Date': str(NOW.date()),
                'AffiliatePartnerName': 'partner_1',
                'Category': 'category_1',
                'OrderType': 'order_type_1_2',
                'Commission': 1.1,
            },
            {
                'Date': str(NOW.date()),
                'AffiliatePartnerName': 'partner_2',
                'Category': 'category_2',
                'OrderType': 'order_type_2_1',
                'Commission': 1.1,
            },
            {
                'Date': str(NOW.date()),
                'AffiliatePartnerName': 'partner_2',
                'Category': 'category_2',
                'OrderType': 'order_type_2_2',
                'Commission': 1.1,
            },
        ],
        '/affiliate_user_commission': [
            {
                'Date': str(DATE_FROM),
                'AffiliatePartnerName': 'partner_1',
                'Category': 'category_1',
                'OrderType': 'order_type_1_1',
                'UserId': 'user_3',
                'Commission': 3.3,
            },
            {
                'Date': str(DATE_FROM),
                'AffiliatePartnerName': 'partner_1',
                'Category': 'category_1',
                'OrderType': 'order_type_1_2',
                'UserId': 'user_3',
                'Commission': 3.3,
            },
            {
                'Date': str(DATE_FROM),
                'AffiliatePartnerName': 'partner_2',
                'Category': 'category_2',
                'OrderType': 'order_type_2_1',
                'UserId': 'user_3',
                'Commission': 3.3,
            },
            {
                'Date': str(DATE_FROM),
                'AffiliatePartnerName': 'partner_2',
                'Category': 'category_2',
                'OrderType': 'order_type_2_2',
                'UserId': 'user_3',
                'Commission': 3.3,
            },
            {
                'Date': str(NOW.date()),
                'AffiliatePartnerName': 'partner_1',
                'Category': 'category_1',
                'OrderType': 'order_type_1_1',
                'UserId': 'user_3',
                'Commission': 3.3,
            },
            {
                'Date': str(NOW.date()),
                'AffiliatePartnerName': 'partner_1',
                'Category': 'category_1',
                'OrderType': 'order_type_1_2',
                'UserId': 'user_3',
                'Commission': 3.3,
            },
            {
                'Date': str(NOW.date()),
                'AffiliatePartnerName': 'partner_2',
                'Category': 'category_2',
                'OrderType': 'order_type_2_1',
                'UserId': 'user_3',
                'Commission': 3.3,
            },
            {
                'Date': str(NOW.date()),
                'AffiliatePartnerName': 'partner_2',
                'Category': 'category_2',
                'OrderType': 'order_type_2_2',
                'UserId': 'user_3',
                'Commission': 3.3,
            },
        ],
    }
    config._read_table = get_read_table_method(table_data)
    config.read_config('')
    assert expected_data['/affiliate_partners'] == config.partners
    assert expected_data['/affiliate_partner_commission'] == config.partner_commission
    assert expected_data['/affiliate_user_commission'] == config.user_commission
