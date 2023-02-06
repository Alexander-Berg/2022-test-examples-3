from travel.avia.stat_admin.tester.testcase import TestCase

from travel.avia.stat_admin.data.models import Settlement, Airport
from travel.avia.stat_admin.lib.title_index import TitleIndex


class TitleIndexTest(TestCase):
    def setUp(self):
        self._index = TitleIndex()

    def test_all(self):
        Settlement(avia_id=1, title='no conflict settlement').save()
        Airport(avia_id=2, title='no conflict airport').save()
        Settlement(avia_id=3, title='conflict').save()
        Airport(avia_id=4, title='conflict').save()

        assert self._index.find('no conflict settlement') == 'c1'
        assert self._index.find('no conflict airport') == 's2'
        assert self._index.find('conflict') == 'c3'
        assert self._index.find('unknown') is None
        assert self._index.find('    conflict    ') == 'c3'

    def test_refetch(self):
        s = Settlement(avia_id=1, title='some')
        s.save()

        assert self._index.find('some') == 'c1'
        s.delete()
        for i in range(100):
            assert self._index.find('some') == 'c1'

        assert self._index.find('some') is None
