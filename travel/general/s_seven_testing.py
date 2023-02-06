# -*- coding: utf-8 -*-
import re
import urllib
from datetime import datetime
from logging import getLogger

import requests
from django.template import loader
from django.conf import settings
from lxml import etree

from travel.avia.ticket_daemon.ticket_daemon.api.flights import Variant
from travel.avia.ticket_daemon.ticket_daemon.daemon.utils import sleep_every
from travel.avia.ticket_daemon.ticket_daemon.lib.baggage import Baggage
from travel.avia.ticket_daemon.ticket_daemon.lib.currency import Price
from travel.avia.ticket_daemon.ticket_daemon.lib.decorators import pipe
from travel.avia.ticket_daemon.ticket_daemon.lib.http import update_query_string
from travel.avia.ticket_daemon.ticket_daemon.lib.tracker import QueryTracker
from travel.avia.ticket_daemon.ticket_daemon.lib.partner_secret_storage import partner_secret_storage

log = getLogger(__name__)

S_SEVEN_API_KEY = partner_secret_storage.get(
    importer_name='s_seven4', namespace='PASSWORD'
)
S_SEVEN_URL = 'https://qa-api.s7airlines.com/mse'

KLASS_MAP = {'economy': u'COACH', 'business': u'BUSINESS', 'first': u'FIRST'}
URL_TRACKER = {
    'utm_source': 'ticket.yandex',
    'utm_medium': 'metasearch',
    'utm_campaign': 'ticket.yandex',
}
SALE_TOTAL_PATTERN = re.compile(r'([A-Z]+)([0-9]+)')

CURRENCY_MAP = settings.AVIA_NATIONAL_CURRENCIES.copy()
CURRENCY_MAP['ru'] = 'RUB'


def validate_query(q):
    q.validate_klass(KLASS_MAP)


@QueryTracker.init_query
def query(tracker, q):
    xml = get_data(tracker, q)

    variants = list(parse_response(xml, q))

    return variants


def build_aviasearch_params(q):
    return {
        'forward_date': q.date_forward.strftime('%Y-%m-%d'),
        'return_date': q.date_backward and q.date_backward.strftime('%Y-%m-%d') or None,
        'key': S_SEVEN_API_KEY,
        'iata_from': q.iata_from,
        'iata_to': q.iata_to,

        'adults': q.passengers.get('adults', 0),
        'children': q.passengers.get('children', 0),
        'infantsInLap': q.passengers.get('infants', 0),

        'cabinType': KLASS_MAP[q.klass],
        'language': 'RU'
    }


def build_xml(xml_template_file, params):
    query_xml = loader.render_to_string(xml_template_file, params)

    return ''.join(filter(None, [
        line.strip() for line in query_xml.splitlines()
    ]))


def get_data(tracker, q):
    query_xml = build_xml(
        'partners/s_seven_testing.xml',
        build_aviasearch_params(q)
    )

    r = tracker.wrap_request(
        requests.post,
        S_SEVEN_URL,
        headers={
            'Content-Type': 'text/xml; charset=utf-8',
            'X-API-Version': '0.52',
        },
        data=query_xml.encode('utf-8'),
        verify=False
    )

    return r.content


def parse_response(xml, q):
    tree = etree.fromstring(xml)
    itineraries = tree.xpath('//result/solutions/itinerary')
    if not itineraries:
        return
    baggage = tree.xpath('//result/includedBaggage')[0]
    fares = tree.xpath('//result/fares')[0]
    for itinerary in sleep_every(itineraries):
        solutions = itinerary.xpath('solution')
        for solution in sleep_every(solutions):
            v = Variant()
            solution_id = solution.get('id')
            m = SALE_TOTAL_PATTERN.match(solution.get('saleTotal'))
            if m:
                currency, price = m.groups()

                if currency not in ['USD', 'EUR', 'UAH']:
                    currency = 'RUR'

                v.tariff = Price(float(price), currency=currency)

            else:
                log.error('Bad price format: %s', solution.get('saleTotal'))
                continue

            v.klass = q.klass

            params = URL_TRACKER.copy()
            params['CUR'] = CURRENCY_MAP.get(q.national_version, 'RUB')

            v.url = urllib.unquote(
                update_query_string(
                    solution.find('ext').get('link'), params
                )
            )

            slices = itinerary.xpath('slice')
            solution_baggage = baggage.find('solution[@id="%s"]' % solution_id)
            baggage_slices = solution_baggage.xpath('slice')
            baggages = get_baggage(solution_baggage)

            fares_solution = fares.find('solution[@id="%s"]' % solution_id)
            tariff_infoes = {segment.get('hash'): segment for segment in fares_solution.xpath('slice/segment')}

            v.forward.segments = parse_segments(
                q.importer.flight_fabric, slices[0], baggage_slices[0], baggages, tariff_infoes
            )

            if q.date_backward:
                v.backward.segments = parse_segments(
                    q.importer.flight_fabric, slices[1], baggage_slices[1], baggages, tariff_infoes
                )

            v.order_data = {'url': v.url}
            yield v


def get_baggage(baggage_node):
    baggages = {}
    for bag in baggage_node.xpath('pricing/checkedBaggageAllowance'):
        if not bag.getparent().get('adults'):
            continue
        baggage_info = bag.find('freeBaggageAllowance')
        if baggage_info is not None:
            baggage = Baggage.from_partner(
                weight=baggage_info.get('kilos'),
                pieces=baggage_info.get('pieces')
            )
            for segment in bag.xpath('segment'):
                baggages[segment.get('hash')] = baggage

    return baggages


@pipe(list)
def parse_segments(flight_fabric, slice, fares, baggage, tariff_infoes):
    for segment, fare in zip(slice.xpath('segment'), fares.xpath('segment')):
        _company_iata = segment.get('carrier')
        tariff_info = tariff_infoes.get(fare.get('hash'), {})
        yield flight_fabric.create(
            company_iata=_company_iata,
            station_from_iata=segment.get('origin'),
            station_to_iata=segment.get('destination'),
            local_departure=datetime.strptime(
                segment.get('departure')[:-6], '%Y-%m-%dT%H:%M'
            ),
            local_arrival=datetime.strptime(
                segment.get('arrival')[:-6], '%Y-%m-%dT%H:%M'
            ),
            pure_number=segment.get('flight')[len(_company_iata):],
            fare_code=tariff_info.get('fareCode'),
            fare_family=tariff_info.get('brandText'),
            baggage=baggage.get(fare.get('hash')),
        )
