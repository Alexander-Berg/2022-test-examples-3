# -*- coding: utf-8 -*-

from collections import namedtuple
import base64
import json

import travel.proto.avia.cpa.label_pb2 as label_pb2


LABEL_FIELD_MAPPING = {
    'unixtime': 'Timestamp',
    'marker': 'Label',
    'national_version': 'NationalVersion',
    'pp': 'Pp',
    'price': 'Price',
    'when': 'When',
    'return_date': 'ReturnDate',
    'adult_seats': 'AdultSeats',
    'children_seats': 'ChildrenSeats',
    'infant_seats': 'InfantSeats',
    'offer_price': 'OfferPrice',
    'utm_source': 'UtmSource',
    'utm_medium': 'UtmMedium',
    'utm_campaign': 'UtmCampaign',
    'utm_content': 'UtmContent',
    'utm_term': 'UtmTerm',
    'wizardRedirKey': 'Wizardredirkey',
    'offer_currency': 'OfferCurrency',
    'ytp_referer': 'YtpReferer',
    'yandexuid': 'YandexUid',
    'passportuid': 'PassportId',
    'klass': 'ServiceClass',
    'affiliateClid': 'AffiliateClid',
    'admitadUid': 'AdmitadUid',
    'travelpayoutsUid': 'TravelpayoutsUid',
    'vid': 'Vid',
    'affiliateVid': 'AffiliateVid',
    'fromId': 'FromId',
    'toId': 'ToId',
}


BaseLabelAvia = namedtuple('BaseLabelAvia', LABEL_FIELD_MAPPING.keys())


BaseLabelHotels = namedtuple('BaseLabelHotels', ['unixtime', 'Label', 'Proto'])


BaseLabelTrain = namedtuple('BaseLabelTrain', ['unixtime', 'LabelHash', 'Proto'])


BaseLabelSuburban = namedtuple('BaseLabelSuburban', ['unixtime', 'LabelHash', 'Proto'])


BaseLabelBuses = namedtuple('BaseLabelBuses', ['unixtime', 'LabelHash', 'Proto'])


BaseLabelTours = namedtuple('BaseLabelTours', ['unixtime', 'LabelHash', 'Proto'])


class MessageMixin(object):

    def as_dict(self, add_data=False):
        d = self._asdict()
        if add_data:
            d['data'] = json.dumps(d)
        return d

    def replace(self, **kwargs):
        return self._replace(**kwargs)

    @classmethod
    def default(cls):
        return cls(*cls.get_defaults())


class LabelAvia(BaseLabelAvia, MessageMixin):

    def get_expected_value(self):
        label_proto = label_pb2.TLabel()
        for key, value in self.as_dict().items():
            if key in ('unixtime', 'marker'):
                continue
            attr_name = LABEL_FIELD_MAPPING.get(key)
            setattr(label_proto, attr_name, value)
        return dict(
            category='avia',
            label=self.marker,
            data=base64.urlsafe_b64encode(label_proto.SerializeToString()),
        )

    @staticmethod
    def get_defaults():
        defaults = [0, '']
        for field in label_pb2.TLabel.DESCRIPTOR.fields:
            defaults.append(field.default_value)
        return defaults


class LabelHotels(BaseLabelHotels, MessageMixin):

    def get_expected_value(self):
        return dict(
            category='hotels',
            label=self.Label,
            data=self.Proto,
        )

    @staticmethod
    def get_defaults():
        return 0, '', ''


class LabelTrain(BaseLabelTrain, MessageMixin):

    def get_expected_value(self):
        return dict(
            category='train',
            label=self.LabelHash,
            data=self.Proto,
        )

    @staticmethod
    def get_defaults():
        return 0, '', ''


class LabelSuburban(BaseLabelSuburban, MessageMixin):

    def get_expected_value(self):
        return dict(
            category='suburban',
            label=self.LabelHash,
            data=self.Proto,
        )

    @staticmethod
    def get_defaults():
        return 0, '', ''


class LabelBuses(BaseLabelBuses, MessageMixin):

    def get_expected_value(self):
        return dict(
            category='buses',
            label=self.LabelHash,
            data=self.Proto,
        )

    @staticmethod
    def get_defaults():
        return 0, '', ''


class LabelTours(BaseLabelTours, MessageMixin):

    def get_expected_value(self):
        return dict(
            category='tours',
            label=self.LabelHash,
            data=self.Proto,
        )

    @staticmethod
    def get_defaults():
        return 0, '', ''


BaseSnapshot = namedtuple('BaseSnapshot', ['partner_name', 'partner_order_id', 'hash', 'updated_at'])


class Snapshot(BaseSnapshot, MessageMixin):

    @staticmethod
    def get_defaults():
        return '', '', '', 0


OrderKey = namedtuple('OrderKey', ['partner_name', 'partner_order_id'])
