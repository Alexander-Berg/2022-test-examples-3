from travel.rasp.library.python.ticket_daemon_client.async_client import TicketDaemonError


class TicketDaemonClientStub:
    def __init__(self, keys=(), values=()):
        self.db = dict(zip(keys, values))

    async def get_prices(self, station_from, station_to, departure_dt, poll, tld='ru', language='ru', include_interlines=False, asker=None):
        key = station_from, station_to, departure_dt.strftime('%Y-%m-%dT%H:%M'), poll, tld, language
        return self.db.get(key)


class TicketDaemonClientExceptionStub:
    def __init__(self, keys=(), values=()):
        self.db = dict(zip(keys, values))

    async def get_prices(self, station_from, station_to, departure_dt, poll, tld='ru', language='ru', include_interlines=False, asker=None):
        key = station_from, station_to, departure_dt.strftime('%Y-%m-%dT%H:%M'), poll, tld, language
        if key in self.db:
            return self.db[key]
        else:
            raise TicketDaemonError('', 499, '')


def create_ticket_daemon_client_json(querying, itineraries, variants, flights):
    _variants = []
    _flights = []

    for forward, keys in itineraries.items():
        _variants.append(
            {
                'partner': 'test_partner',
                'forward': forward,
                'tariff': {'currency': 'RUB', 'value': variants[forward]},
                'deep_link': 'https://travel.yandex.ru/avia/',
                'order_link': 'https://travel.yandex.ru/avia/order/?utm_source=rasp&lang=ru&utm_campaign=order_link'
            }
        )
        for key in keys:
            from_id, to_id, departure, arrival, number = flights[key]
            _flights.append({
                'key': key,
                'station_from': from_id,
                'station_to': to_id,
                'departure': {
                    'local': '{}:00'.format(departure),
                    'tzname': 'UTC'
                },
                'arrival': {
                    'local': '{}:00'.format(arrival),
                    'tzname': 'UTC'
                },
                'number': 'plane_{}'.format(number),
                'url': 'https://avia.yandex.ru/{}'.format(number)
            })
    return {
        'data': {
            'status': {
                'test_partner': 'querying' if querying else 'done'
            },
            'variants': _variants,
            'reference': {
                'itineraries': itineraries,
                'flights': _flights
            }
        }
    }
