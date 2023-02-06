import json

from travel.avia.flight_status_fetcher.library.raw_data import StatusDataPack
from travel.avia.flight_status_fetcher.services.status import Status


def test_status_data_pack_json_format():
    status = Status(
        message_id='message_id',
        received_at=123,
        airport='apt',
        airline_id=1001,
        airline_code='acd',
        flight_number='fn1',
        flight_date=None,
        direction='arrival',
        time_actual=None,
        time_scheduled=None,
        status='',
        gate='',
        terminal='',
        check_in_desks='',
        baggage_carousels='',
        source='src',
    )
    sdp = StatusDataPack(
        message_id='message_id',
        received_at=123,
        statuses=[status],
        partner='partner',
        data='data',
        error='error',
    )
    expected = {
        'message_id': 'message_id',
        'received_at': 123,
        'statuses': [status.fields_dict],
        'partner': 'partner',
        'data': 'data',
        'error': 'error',
    }
    assert expected == json.loads(sdp.to_json_bytes())
