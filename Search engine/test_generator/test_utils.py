from search.martylib.core.date_utils import get_datetime

from search.morty.proto.structures import component_pb2, process_pb2

from search.morty.src.model.process.utils import connect_subprocesses, calendar_to_timeline

from search.morty.tests.utils.test_case import MortyTestCase


class TestGeneratorUtils(MortyTestCase):
    def test_connect_subprocesses(self):
        parents = [process_pb2.SubProcess(id='1', required=['0']), process_pb2.SubProcess(id='2', required=['1'])]
        childes = [process_pb2.SubProcess(id='3', required=['x']), process_pb2.SubProcess(id='4', required=['3'])]
        connect_subprocesses(parents, childes)

        assert list(parents[0].required) == ['0']
        assert list(parents[1].required) == ['1']
        assert list(childes[0].required) == ['x', '2']
        assert list(childes[1].required) == ['3']

    def test_calendar_to_timeline(self):
        # test one day
        calendar = component_pb2.Calendar(
            intervals=[
                component_pb2.Calendar.Interval(
                    start=10,
                    duration=2,
                    days=[
                        component_pb2.Calendar.Day.MONDAY,
                    ]
                )
            ]
        )
        res = calendar_to_timeline(calendar)
        assert len(res) == 2 * 1
        for item in res:
            date = get_datetime(item.start)
            assert date.weekday() == 0
            assert item.end - item.start == 2 * 3600
            assert item.start % (24 * 3600) == (10 - 3) * 3600

        # test all week
        calendar = component_pb2.Calendar(
            intervals=[
                component_pb2.Calendar.Interval(
                    start=10,
                    duration=2,
                    days=[
                        component_pb2.Calendar.Day.ALL_WEEK,
                    ]
                )
            ]
        )
        res = calendar_to_timeline(calendar)
        assert len(res) == 2 * 7
        for i, item in enumerate(sorted(res, key=lambda x: x.start)):
            date = get_datetime(item.start)
            assert date.weekday() == i % 7

        # test work day
        calendar = component_pb2.Calendar(
            intervals=[
                component_pb2.Calendar.Interval(
                    start=10,
                    duration=2,
                    days=[
                        component_pb2.Calendar.Day.WORKDAY,
                    ]
                )
            ]
        )
        res = calendar_to_timeline(calendar)
        assert len(res) == 2 * 5
        for i, item in enumerate(sorted(res, key=lambda x: x.start)):
            date = get_datetime(item.start)
            assert date.weekday() == i % 5

        # test two days
        calendar = component_pb2.Calendar(
            intervals=[
                component_pb2.Calendar.Interval(
                    start=10,
                    duration=2,
                    days=[
                        component_pb2.Calendar.Day.MONDAY,
                        component_pb2.Calendar.Day.TUESDAY,
                    ]
                )
            ]
        )
        res = calendar_to_timeline(calendar)
        assert len(res) == 2 * 2
        for i, item in enumerate(sorted(res, key=lambda x: x.start)):
            date = get_datetime(item.start)
            assert date.weekday() == i % 2
