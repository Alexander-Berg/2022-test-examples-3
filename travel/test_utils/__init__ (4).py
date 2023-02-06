from travel.rasp.library.python.train_api_client.async_client import TrainApiError


class TrainApiClientStub:
    def __init__(self, keys=(), values=()):
        self.db = dict(zip(keys, values))

    async def get_prices(self, station_from, station_to, departure_dt, poll, tld='ru', language='ru', asker=None):
        key = station_from, station_to, departure_dt.strftime('%Y-%m-%dT%H:%M'), poll, tld, language
        return self.db.get(key, {"segments": [], "querying": True})


class TrainApiClientExceptionStub:
    def __init__(self, keys=(), values=()):
        self.db = dict(zip(keys, values))

    async def get_prices(self, station_from, station_to, departure_dt, poll, tld='ru', language='ru', asker=None):
        key = station_from, station_to, departure_dt.strftime('%Y-%m-%dT%H:%M'), poll, tld, language
        if key in self.db:
            return self.db[key]
        else:
            raise TrainApiError('', 499, '')


def create_train_api_client_json(querying, segments):
    segments = [
        {
            'stationFrom': {'id': from_id},
            'stationTo': {'id': to_id},
            'tariffs': {
                'classes': {
                    'platzkarte': {
                        'price': {'currency': 'RUB', 'value': price},
                        'trainOrderUrl': '/'
                    }
                },
            },
            'departure': '{}:00+00:00'.format(departure),
            'arrival': '{}:00+00:00'.format(arrival),
            'thread': {'number': 'train_{}'.format(number)},
            'hasDynamicPricing': True,
            'rawTrainName': None,
            'provider': 'P1'
        }
        for from_id, to_id, price, departure, arrival, number in segments]
    return {
        'querying': querying,
        'segments': segments
    }
