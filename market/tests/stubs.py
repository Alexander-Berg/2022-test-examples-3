# -*- coding: utf-8 -*-

from market.proto.delivery.delivery_calc import delivery_calc_pb2


class CurrencyExchangeStub(object):
    @staticmethod
    def get_rate(bank, currency_from, currency_to):
        if currency_from == currency_to:
            return 1.0
        elif currency_from == 'USD' and currency_to == 'RUR':
            return 10.0
        else:
            return 0.1

    @staticmethod
    def get_currency_by_region(region, default=''):
        return 'RUR' if region == 5 else 'USD' if region == 6 else default

    @staticmethod
    def get_bank_by_region(region, default=''):
        return 'CBRF' if region == 5 else 'NBRB' if region == 6 else default


class OfferDataStub(object):
    def __init__(self):
        class YmlOptionStub(object):
            def __init__(self, cost, days_min, days_max, order_before):
                self.Cost = cost
                self.DaysMin = days_min
                self.DaysMax = days_max
                self.OrderBeforeHour = order_before

        self.DeliveryCurrency = 'USD'
        self.priority_regions = '3'
        self.UseYmlDelivery = True
        self.DeliveryOptions = [
            YmlOptionStub(3, 2, 3, 20),
            YmlOptionStub(2, 1, 10, 24),
        ]


def dc_bucket(b_id, currency, regs):
    """ Get DeliveryCalc bucket """
    result = delivery_calc_pb2.DeliveryOptionsBucket()
    result.delivery_opt_bucket_id = b_id
    result.currency = currency
    for r in regs:
        region = result.delivery_option_group_regs.add()
        region.region = r['region']
        region.delivery_opt_group_id = r['group_id']
        region.option_type = r['type']
    return result


def dc_option(cost, min_days, max_days, order_before):
    """ Get DeliveryCalc option """
    opt = delivery_calc_pb2.DeliveryOption()
    opt.delivery_cost = cost
    opt.min_days_count = min_days
    opt.max_days_count = max_days
    opt.order_before = order_before
    return opt


def dc_option_group(g_id, options):
    """ Get DeliveryCalc option group """
    result = delivery_calc_pb2.DeliveryOptionsGroup()
    result.delivery_option_group_id = g_id
    for o in options:
        opt = result.delivery_options.add()
        opt.CopyFrom(o)
    return result
