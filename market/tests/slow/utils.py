# coding: utf-8

from market.idx.datacamp.proto.offer.DataCampOffer_pb2 import (
    Offer, OfferIdentifiers, OfferPrice, PriceBundle, OfferContent,
    PartnerContent, OfferPictures, PartnerPictures, PartnerInfo,
    OriginalSpecification, StringValue, SourcePicture, SourcePictures
)
from market.idx.datacamp.yatf.utils import dict2tskv
from market.idx.datacamp.yatf.utils import create_meta
from market.idx.pylibrary.yml_feed.generate_yml_feed import YmlFeed
from market.proto.common.common_pb2 import PriceExpression


class DataCampShop(object):
    def __init__(self, shop_id, shop_info, offers, categories=None, currencies=None, services=None, promos=None, gifts=None):
        self.shop_id = shop_id
        self.shop_info = shop_info
        self.offer_dicts = offers
        self.datacamp_offers = []
        self._offer_dicts_to_datacamp_offer()
        self.category_dicts = categories
        self.currency_dicts = currencies
        self.service_dicts = services
        self.promos_dicts = promos
        self.gifts_dicts = gifts

    @staticmethod
    def default_shop_dict(**kwargs):
        default_shop = {
            'name': 'TestCompany',
            'company': 'Test Company',
            'date': '1970-01-01 00:00',
            'url': 'https://www.tesUrl.ru/testShop/'
        }
        default_shop.update(**kwargs)

        return default_shop

    @staticmethod
    def default_currency_dict(**kwargs):
        default_currency = {
            'id': 'RUR',
            'rate': '1',
        }
        default_currency.update(**kwargs)

        return default_currency

    @staticmethod
    def default_category_dict(**kwargs):
        default_category = {
            'id': '1',
            'value': 'CategoryDesc',
        }
        default_category.update(**kwargs)

        return default_category

    @staticmethod
    def default_offer_dict(**kwargs):
        default_offer = {
            'id': 'TestOfferId',
            'name': 'TestTitle',
            'url': 'https://www.tesUrl.ru/testOffer/',
            'price': '150',
            'currencyId': 'RUR',
            'categoryId': '1',
            'delivery': 'false',
            'picture': ['https://www.tesUrl.ru/testOfferPic/'],
        }
        default_offer.update(**kwargs)

        return default_offer

    def offer_price(self, offer_dict):
        rate = offer_dict.pop('price_rate') if 'price_rate' in offer_dict else '1'
        plus = offer_dict.pop('price_plus') if 'price_plus' in offer_dict else 0
        ref_id = offer_dict.pop('price_ref_id') if 'price_ref_id' in offer_dict else 'RUR'
        return PriceExpression(
            price=int(float(offer_dict['price']) * 10000000),
            id=offer_dict['currencyId'],
            rate=rate,
            plus=plus,
            ref_id=ref_id,
        )

    def _offer_dicts_to_datacamp_offer(self):
        for offer_dict in self.offer_dicts:
            offer = Offer(
                identifiers=OfferIdentifiers(
                    shop_id=self.shop_id,
                    offer_id=offer_dict['id']
                ),
                meta=create_meta(10),
                content=OfferContent(
                    partner=PartnerContent(
                        original=OriginalSpecification(
                            name=StringValue(
                                value=offer_dict['name'],
                            ),
                            url=StringValue(
                                value=offer_dict['url'],
                            ),
                        )
                    ),
                ),
                pictures=OfferPictures(
                    partner=PartnerPictures(
                        original=SourcePictures(
                            source=[SourcePicture(url=url) for url in offer_dict['picture']]
                        )
                    )
                ),
                price=OfferPrice(
                    basic=PriceBundle(
                        binary_price=self.offer_price(offer_dict)
                    )
                ),
                partner_info=PartnerInfo()
            )
            self.datacamp_offers.append(offer)

    def generate_yml_feed(self):
        yml_feed = YmlFeed.from_dicts(
            self.offer_dicts,
            self.shop_info,
            self.category_dicts,
            self.currency_dicts,
            self.service_dicts,
            self.promos_dicts,
            self.gifts_dicts
        )
        return yml_feed.root_element_tree

    def partners_table_row(self):
        return {
            'shop_id': self.shop_id,
            'status': 'publish',
            'mbi': dict2tskv({
                'shop_id': self.shop_id,
                'datafeed_id': self.shop_id,
                'regions': '213',
                'shopname': self.shop_info['name'],
                'datasource_name': self.shop_info['company'],
                'domain': self.shop_info['url'],
                'is_push_partner': 'true'
            })
        }
