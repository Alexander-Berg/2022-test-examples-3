from collections import Counter


from common.tester.factories import create_thread, create_station
from common.tester.testcase import TestCase
from travel.rasp.library.python.common23.date.environment import now_aware

from travel.rasp.suburban_widget.suburban_widget.views.suburban import next_suburban_trains


class TestNextSuburbanTrains(TestCase):
    def test_express(self):
        station_from, station_to = create_station(), create_station()

        expresses = ['express', 'aeroexpress', None, None, 'express', 'express', 'aeroxepress']
        for express_type in expresses:
            create_thread(
                t_type='suburban',
                __={'calculate_noderoute': True},
                schedule_v1=[
                    [None, 0, station_from],
                    [43, None, station_to]
                ],
                express_type=express_type,
            )

        result = next_suburban_trains(station_from, station_to, 10, now_aware())

        expresses_in_result = Counter(v['express'] for v in result)
        assert expresses_in_result[True] == 3
        assert expresses_in_result[False] == 4
