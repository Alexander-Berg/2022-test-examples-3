import datetime as dt

from extsearch.video.robot.tools.library.python import solomon


class FakeReporter(solomon.SolomonPushApiReporter):

    def __init__(self):
        self.values = []

    def set_value(self, sensor, value, labels, ts_datetime):
        self.values.append({
            'sensor': sensor,
            'value': value,
            'labels': labels,
            'ts_datetime': [item.replace(tzinfo=dt.timezone.utc).timestamp() for item in ts_datetime],
        })

    def get_values(self):
        return self.values


class TestSolomon(object):

    def test_empty(self):
        reporter = FakeReporter()
        batched_reporter = solomon.BatchedSolomonPushApiReporter(reporter=reporter)
        batched_reporter.finish()
        assert reporter.get_values() == []

    def test_simple(self):
        reporter = FakeReporter()
        batched_reporter = solomon.BatchedSolomonPushApiReporter(reporter=reporter)
        ts_datetime = dt.datetime.utcfromtimestamp(1598391491)
        batched_reporter.add_value('cat', 1, {'color': 'white'}, ts_datetime)
        batched_reporter.add_value('dog', 2, {'color': 'black'}, ts_datetime)
        batched_reporter.add_value('mouse', 3, {'color': 'gray'}, ts_datetime)
        batched_reporter.finish()
        assert reporter.get_values() == [
            {
                'sensor': ['cat', 'dog', 'mouse'],
                'value': [1, 2, 3],
                'labels': [{'color': 'white'}, {'color': 'black'}, {'color': 'gray'}],
                'ts_datetime': [1598391491, 1598391491, 1598391491],
            },
        ]

    def test_max_values_per_batch(self):
        reporter = FakeReporter()
        batched_reporter = solomon.BatchedSolomonPushApiReporter(reporter=reporter, max_values_per_batch=2)
        ts_datetime = dt.datetime.utcfromtimestamp(1598391491)
        batched_reporter.add_value('cat', 1, {'color': 'white'}, ts_datetime)
        batched_reporter.add_value('dog', 2, {'color': 'black'}, ts_datetime)
        batched_reporter.add_value('mouse', 3, {'color': 'gray'}, ts_datetime)
        batched_reporter.finish()
        assert reporter.get_values() == [
            {
                'sensor': ['cat', 'dog'],
                'value': [1, 2],
                'labels': [{'color': 'white'}, {'color': 'black'}],
                'ts_datetime': [1598391491, 1598391491],
            },
            {
                'sensor': ['mouse'],
                'value': [3],
                'labels': [{'color': 'gray'}],
                'ts_datetime': [1598391491],
            },
        ]
