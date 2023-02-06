import logging

from travel.avia.flight_status_fetcher.settings.app import AIRPORT_IMPORTER_RETRY_SETTINGS
from travel.avia.flight_status_fetcher.settings.sources.dme import DME_REQUEST_TIMEOUT
from travel.avia.flight_status_fetcher.sources.dme import DMEImporter
from travel.avia.flight_status_fetcher.tests import basic_status_asserts
from travel.avia.flight_status_fetcher.tests.test_sources.helpers import (
    delete_fields_from_dict,
    mock_flight_number_parser,
)

logger = logging.getLogger(__name__)

sample_xml = '''
<DMDTablo>
    <DMDTabloArrive>
        <DMDTabloArriveFlight
            NAT="PAX"
            APPN_ROW_ID="259878226"
            FL_NUM_E="B2 953"
            FL_NUM_R="В2 953"
            FL_NUM_PUB="B2 953"
            Master_FL_NUM="В2 953"
            Master_FL_NUM_E="B2 953"
            AIRCOMPANY_E="Belavia-Belarusian Airlines JSC"
            AIRCOMPANY_R="ОАО &quot;Авиакомпания &quot;Белавиа&quot;"
            ORG_E="EKATERINBURG Koltsovo"
            ORG_R="ЕКАТЕРИНБУРГ Кольцово"
            ROUTE_E="EKATERINBURG Koltsovo-&gt;MINSK International-&gt;Moscow(Domodedovo)"
            ROUTE_R="ЕКАТЕРИНБУРГ Кольцово-&gt;МИНСК Интернэшнл-&gt;Москва(Домодедово)"
            DELAYED="0"
            CANCELLED="0"
            RCS_DESC="Прибыл"
            RCS_DESC_E="Arrived"
            TIM_P="2020-01-27T00:05:00"
            TIM_F="2020-01-27T00:01:00"
            TIM_D="2020-01-27T00:08:00"
            TIM_L="2020-01-27T00:10:00"
            STATUS="3"
        />
        <DMDTabloArriveFlight
            NAT="PAX"
            APPN_ROW_ID="442445066"
            FL_NUM_E="RA67711"
            FL_NUM_R="RA67711"
            Master_FL_NUM="RA67711"
            Master_FL_NUM_E="RA67711"
            AIRCOMPANY_E="General (Business) Aviation"
            AIRCOMPANY_R="Общая (Бизнес) Авиация"
            ORG_E="NOVINKI Aviacenter &quot;Poletaem&quot;"
            ORG_R="НОВИНКИ Авиацентр &quot;Полетаем&quot;"
            ROUTE_E="NOVINKI Aviacenter &quot;Poletaem&quot;-&gt;Moscow(Domodedovo)"
            ROUTE_R="НОВИНКИ Авиацентр &quot;Полетаем&quot;-&gt;Москва(Домодедово)"
            DELAYED="0"
            CANCELLED="0"
            TIM_P="2020-12-11T11:20:00"
            TIM_F="2020-12-11T11:20:00"
            TIM_L="2020-12-11T11:20:00"
            STATUS="0"
        />
    </DMDTabloArrive>
    <DMDTabloDeparture>
        <DMDTabloDepartureFlight
            NAT="PAX"
            APPN_ROW_ID="261509406"
            FL_NUM_E="EY 8679"
            FL_NUM_R="EY 8679"
            FL_NUM_PUB="EY 8679"
            MCSF="С72571"
            Master_FL_NUM="С72571"
            Master_FL_NUM_E="С72571"
            AIRCOMPANY_E="Etihad Airways"
            AIRCOMPANY_R="Etihad Airways"
            DES_E="EKATERINBURG Koltsovo"
            DES_R="ЕКАТЕРИНБУРГ Кольцово"
            ROUTE_E="Moscow(Domodedovo)-&gt;NOVY URENGOJ-&gt;EKATERINBURG Koltsovo"
            ROUTE_R="Москва(Домодедово)-&gt;НОВЫЙ УРЕНГОЙ-&gt;ЕКАТЕРИНБУРГ Кольцово"
            DELAYED="0"
            CANCELLED="0"
            TIM_P="2020-01-29T02:05:00"
            TIM_F="2020-01-29T02:05:00"
            TIM_L="2020-01-29T02:05:00"
            STATUS="8"
            REGPOINT="109-118"
        />
    </DMDTabloDeparture>
</DMDTablo>
'''


def test_get_dme_flights():
    importer = DMEImporter(
        'no-url',
        logger=logger,
        flight_number_parser=mock_flight_number_parser(
            logger,
            {
                'B2': 7,
                'EY': 123456,
            },
        ),
        retry_settings=AIRPORT_IMPORTER_RETRY_SETTINGS,
        request_timeout=DME_REQUEST_TIMEOUT,
    )
    flights = list(importer.collect_statuses(sample_xml).statuses)

    basic_status_asserts(importer, flights, 2)

    result = flights[0].fields_dict
    assert delete_fields_from_dict(result) == {
        'airline_id': 7,
        'airline_code': 'B2',
        'airport': 'DME',
        'baggage_carousels': None,
        'check_in_desks': None,
        'direction': 'arrival',
        'diverted': False,
        'diverted_airport_iata': None,
        'diverted_airport_code': None,
        'flight_date': '2020-01-27',
        'flight_number': '953',
        'gate': None,
        'route_point_from': 'МИНСК Интернэшнл',
        'route_point_to': 'DME',
        'source': 'airport',
        'status': 'unknown',
        'terminal': None,
        'time_actual': '2020-01-27T00:10:00',
        'time_scheduled': '2020-01-27T00:05:00',
    }

    result = flights[1].fields_dict
    assert delete_fields_from_dict(result) == {
        'airline_id': 123456,
        'airline_code': 'EY',
        'airport': 'DME',
        'baggage_carousels': None,
        'check_in_desks': '109-118',
        'direction': 'departure',
        'diverted': False,
        'diverted_airport_iata': None,
        'diverted_airport_code': None,
        'flight_date': '2020-01-29',
        'flight_number': '8679',
        'gate': None,
        'route_point_from': 'DME',
        'route_point_to': 'НОВЫЙ УРЕНГОЙ',
        'source': 'airport',
        'status': 'unknown',
        'terminal': None,
        'time_actual': '2020-01-29T02:05:00',
        'time_scheduled': '2020-01-29T02:05:00',
    }
