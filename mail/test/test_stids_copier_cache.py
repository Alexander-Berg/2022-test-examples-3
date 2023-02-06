from contextlib import contextmanager
from collections import namedtuple
import mock
import pytest
from mail.pypg.pypg.fake_cursor import FakeCursor
from pymdb.types import MailMimePart
from ora2pg.clone_user.stids_copier import StidWithMime
from ora2pg.clone_user.stids_copier_cache import STIDsCopierCache


XML_MIME = '''<?xml version="1.0" encoding="windows-1251"?>
<message>
<part id="1" offset="1" length="9">
</part>
</message>
'''

MIME_PARTS = [MailMimePart(
    hid='1',
    content_type='',
    content_subtype='',
    boundary='',
    name='',
    charset='',
    encoding='',
    content_disposition='',
    filename='',
    cid='',
    offset_begin=1,
    offset_end=10
)]


class TestSTIDsCopierCache(object):
    MPATH = 'ora2pg.clone_user.stids_copier_cache'
    TEST_UID = 42
    SCCMocks = namedtuple('SCCMocks', ['scc', 'qexec', 'conn'])

    @contextmanager
    def mock_it(self, *rows):
        with mock.patch(
            self.MPATH + '.qexec',
            autospec=True,
        ) as qexec_mock:
            qexec_mock.return_value = FakeCursor(
                ['st_id', 'new_st_id', 'mime_xml'],
                rows or []
            )
            conn = mock.MagicMock()
            yield self.SCCMocks(
                STIDsCopierCache(
                    conn=conn,
                    uid=self.TEST_UID),
                qexec_mock,
                conn,
            )

    def test_empty_do_not_contains_copies(self):
        with self.mock_it() as m:
            assert 'fake_stid' not in m.scc

    def test_getitem_produrce_key_error_when_no_such_item(self):
        with self.mock_it() as m:
            with pytest.raises(KeyError):
                m.scc['fake_stid']

    def test_get_when_no_such_item_return_default(self):
        with self.mock_it() as m:
            assert m.scc.get('foo_stid', StidWithMime('bar_stid', None, None)) == StidWithMime('bar_stid', None, None)

    def test_contains_item_when_it_exists_in_db_cache(self):
        with self.mock_it(['orig_stid', 'new_stid', None]) as m:
            assert 'orig_stid' in m.scc

    @pytest.mark.parametrize(('mime_xml', 'mime'), [
        (None, None),
        (XML_MIME, MIME_PARTS),
    ])
    def test_getitem_when_it_exists_in_db_cache(self, mime_xml, mime):
        with self.mock_it(['orig_stid', 'new_stid', mime_xml]) as m:
            assert m.scc['orig_stid'] == StidWithMime('new_stid', mime_xml, mime)

    def test_contains_item_after_set_it(self):
        with self.mock_it() as m:
            m.scc['orig'] = StidWithMime('new', None, None)
            assert 'orig' in m.scc

    @pytest.mark.parametrize(('mime_xml', 'mime'), [
        (None, None),
        (XML_MIME, MIME_PARTS),
    ])
    def test_get_item_after_set_it(self, mime_xml, mime):
        with self.mock_it() as m:
            m.scc['orig'] = StidWithMime('new', mime_xml, mime)
            assert m.scc.get('orig', StidWithMime('stange-orig', None, None)) == StidWithMime('new', mime_xml, mime)
