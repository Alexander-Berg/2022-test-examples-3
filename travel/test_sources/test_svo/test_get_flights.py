import json
import logging
from datetime import date

from travel.avia.flight_status_fetcher.sources.svo import SVOImporter
from travel.avia.flight_status_fetcher.tests import basic_status_asserts
from travel.avia.flight_status_fetcher.tests.test_sources.helpers import (
    delete_fields_from_dict,
    mock_flight_number_parser,
)

logger = logging.getLogger(__name__)


class FakeFetcher(object):
    def fetch(self, *args, **kwargs):
        sample_content = json.loads(
            '''
            [
                {
                "ad": "A",
                "co": "SU",
                "flight": "1383",
                "dat": "2020-01-26T21:00:00Z",
                "id": "D",
                "iId": 7288740,
                "mar1": "HMA",
                "mar1Id": "332",
                "mar1At": null,
                "mar1Dt": "2020-01-27T02:15:00Z",
                "mar2": "SVO",
                "mar2Id": "273",
                "mar2At": null,
                "mar2Dt": null,
                "mar3": null,
                "mar3Id": null,
                "mar3At": null,
                "mar3Dt": null,
                "mar4": null,
                "mar4Id": null,
                "mar4At": null,
                "mar4Dt": null,
                "mar5": null,
                "mar5Id": null,
                "mar5At": null,
                "mar5Dt": null,
                "tAt": "2020-01-27T05:01:35Z",
                "terminal": "D",
                "tSt": "2020-01-27T05:00:00Z",
                "tEt": "2020-01-27T05:00:00Z",
                "chinId": null,
                "gateId": null,
                "baggageBeltId": "3",
                "baggageBeltStart": "2020-01-27T05:29:00Z",
                "baggageBeltFinish": "2020-01-27T05:44:00Z",
                "statusDetailedEng": "Landed 08:01",
                "statusDetailedRus": "Совершил посадку в 08:01",
                "statusDetailedChn": "已经着陆 08:01",
                "statusCode": "12",
                "estimated_chinStart": null,
                "estimated_chin_finish": null,
                "estimated_bagStart": "2020-01-27T05:33:00Z",
                "termGate": "D",
                "statusId": 70
                },
                {
                "ad": "A",
                "co": "SU",
                "flight": "1383",
                "dat": "2020-02-26T21:00:00Z",
                "id": "D",
                "iId": 7288741,
                "mar1": "XXX",
                "mar1Id": "123",
                "mar1At": null,
                "mar1Dt": "2020-02-27T02:15:00Z",
                "mar2": "SVO",
                "mar2Id": "273",
                "mar2At": null,
                "mar2Dt": null,
                "mar3": null,
                "mar3Id": null,
                "mar3At": null,
                "mar3Dt": null,
                "mar4": null,
                "mar4Id": null,
                "mar4At": null,
                "mar4Dt": null,
                "mar5": null,
                "mar5Id": null,
                "mar5At": null,
                "mar5Dt": null,
                "tAt": "2020-02-27T05:01:35Z",
                "terminal": "D",
                "tSt": "2020-02-27T05:00:00Z",
                "tEt": "2020-02-27T05:00:00Z",
                "chinId": null,
                "gateId": null,
                "baggageBeltId": "3",
                "baggageBeltStart": "2020-02-27T05:29:00Z",
                "baggageBeltFinish": "2020-02-27T05:44:00Z",
                "statusDetailedEng": "Landed 08:01",
                "statusDetailedRus": "Совершил посадку в 08:01",
                "statusDetailedChn": "已经着陆 08:01",
                "statusCode": "13",
                "termGate": "D",
                "statusId": 71
                },
                {
                "ad": "A",
                "co": "XX",
                "flight": "MAF003",
                "dat": "2020-09-03T21:00:00Z",
                "id": "I",
                "iId": 7596217,
                "mar1": "ZGC",
                "mar1Id": "1615",
                "mar1At": null,
                "mar1Dt": null,
                "mar2": "OVB",
                "mar2Id": "126",
                "mar2At": null,
                "mar2Dt": null,
                "mar3": "SVO",
                "mar3Id": "273",
                "mar3At": null,
                "mar3Dt": null,
                "mar4": null,
                "mar4Id": null,
                "mar4At": null,
                "mar4Dt": null,
                "mar5": null,
                "mar5Id": null,
                "mar5At": null,
                "mar5Dt": null,
                "tAt": null,
                "terminal": "F",
                "tSt": "2020-09-04T16:00:00Z",
                "tEt": null,
                "chinId": null,
                "gateId": null,
                "baggageBeltId": "3",
                "baggageBeltStart": null,
                "baggageBeltFinish": null,
                "statusDetailedEng": null,
                "statusDetailedRus": null,
                "statusDetailedChn": null,
                "pcc": "C",
                "statusCode": null,
                "termGate": null,
                "statusId": null
                }
            ]
        '''
        )
        yield from sample_content


def test_get_flights():
    importer = SVOImporter(
        from_date=date(2020, 1, 1),
        to_date=date(2020, 1, 1),
        fetcher=FakeFetcher(),
        logger=logger,
        flight_number_parser=mock_flight_number_parser(
            logger,
            {
                'SU': 26,
                'MAF': 1,
            },
        ),
    )
    flights = importer.collect_statuses().statuses
    assert len(flights) == 2

    basic_status_asserts(importer, flights, 2, data_size=3)

    assert delete_fields_from_dict(flights[0].fields_dict) == {
        'airline_id': 26,
        'airline_code': 'SU',
        'airport': 'SVO',
        'baggage_carousels': '3',
        'check_in_desks': None,
        'direction': 'arrival',
        'diverted': False,
        'diverted_airport_iata': None,
        'diverted_airport_code': None,
        'flight_date': '2020-01-27',
        'flight_number': '1383',
        'gate': None,
        'route_point_from': 'HMA',
        'route_point_to': 'SVO',
        'source': 'airport',
        'status': 'arrived',
        'terminal': 'D',
        'time_actual': '2020-01-27T08:01:35',
        'time_scheduled': '2020-01-27T08:00:00',
    }

    assert delete_fields_from_dict(flights[1].fields_dict) == {
        'airline_id': 1,
        'airline_code': 'MAF',
        'airport': 'SVO',
        'baggage_carousels': '3',
        'check_in_desks': None,
        'direction': 'arrival',
        'diverted': False,
        'diverted_airport_iata': None,
        'diverted_airport_code': None,
        'flight_date': '2020-09-04',
        'flight_number': '3',
        'gate': None,
        'route_point_from': 'OVB',
        'route_point_to': 'SVO',
        'source': 'airport',
        'status': 'no-data',
        'terminal': 'F',
        'time_actual': None,
        'time_scheduled': '2020-09-04T19:00:00',
    }
