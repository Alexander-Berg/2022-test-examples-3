from contextlib import contextmanager
from collections import namedtuple
from ora2pg.clone_user.stids_copier import STIDsCopier, StidWithMime
from ora2pg.storage import MulcaGate
from ora2pg import storage, mime_xml as MX
from pymdb.types import MailMimePart
import mock
import pytest

MULCAGATE_HOST = 'test://mulcagate'
MULCAGATE_PORT = 12345
USER_ID = 'Eve'
CA_FILE_PATH = '//path/to/ca'
MULCAGATE = storage.MulcaGate(
    host=MULCAGATE_HOST,
    port=MULCAGATE_PORT,
    mg_ca_path=CA_FILE_PATH,
)

XML_MIME = b'''<?xml version="1.0" encoding="windows-1251"?>
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

MESSAGE = b'''
From: Alice@ya.ru
To: Bob@ya.ru
Subject: http://www.catonmat.cd /

Alice is sending her message to Bob
Protecting that transmission is Crypto's job
Without the help of our good friend Trent,
It's hard to get that secret message sent
Work tries to deposit the check of your salary
But with no crypto, it'll be changed by Mallory
You think no one will see what it is, you believe?
But you should never forget, there's always an Eve...

'''

OLD_MESSAGE = XML_MIME + MESSAGE

MulcagateMocks = namedtuple('MulcagateMocks', ['get_raw_data', 'put'])


@contextmanager
def patched_mulcagate():
    with \
        mock.patch.object(MulcaGate, 'get_raw_data') as mock_get_raw_data, \
        mock.patch.object(MulcaGate, 'put') as mock_put \
    :
        yield MulcagateMocks(mock_get_raw_data, mock_put)


@pytest.mark.parametrize('copy_call_count', [1, 2])
def test_copier_when_all_ok_for_new_messages(copy_call_count):
    with patched_mulcagate() as mg:
        mg.get_raw_data.side_effect = [MESSAGE]
        mg.put.side_effect = ['new.Eve.stid']

        copier = STIDsCopier(MULCAGATE, USER_ID)
        for _ in range(copy_call_count):
            assert copier.copy('orig.Alice.stid') == StidWithMime('new.Eve.stid', None, None)

        mg.get_raw_data.assert_called_once_with(
            st_id='orig.Alice.stid',
        )
        mg.put.assert_called_once_with(
            base_id=USER_ID,
            data=MESSAGE,
        )


@pytest.mark.parametrize('copy_call_count', [1, 2])
def test_copier_when_all_ok_for_old_messages(copy_call_count):
    with patched_mulcagate() as mg:
        mg.get_raw_data.side_effect = [OLD_MESSAGE]
        mg.put.side_effect = ['new.Eve.stid']

        copier = STIDsCopier(MULCAGATE, USER_ID)
        for _ in range(copy_call_count):
            assert copier.copy('orig.Alice.stid') == StidWithMime('new.Eve.stid', XML_MIME, MIME_PARTS)

        mg.get_raw_data.assert_called_once_with(
            st_id='orig.Alice.stid',
        )
        mg.put.assert_called_once_with(
            base_id=USER_ID,
            data=MESSAGE,
        )


def test_copier_return_original_stid_when_mulca_get_fail():
    with patched_mulcagate() as mg:
        mg.get_raw_data.side_effect = [storage.StorageGetError('')]

        copier = STIDsCopier(MULCAGATE, USER_ID)
        assert copier.copy('orig.Alice.stid') == StidWithMime('orig.Alice.stid', None, None)


def test_copier_return_original_stid_when_mulca_put_fail():
    with patched_mulcagate() as mg:
        mg.get_raw_data.side_effect = [MESSAGE]
        mg.put.side_effect = [storage.StoragePutError('')]

        copier = STIDsCopier(MULCAGATE, USER_ID)
        assert copier.copy('orig.Alice.stid') == StidWithMime('orig.Alice.stid', None, None)


def test_copier_return_original_stid_when_parse_mime_xml_fail():
    with patched_mulcagate() as mg:
        mg.get_raw_data.side_effect = [MX.MimeXmlError('')]

        copier = STIDsCopier(MULCAGATE, USER_ID)
        assert copier.copy('orig.Alice.stid') == StidWithMime('orig.Alice.stid', None, None)


@pytest.mark.parametrize(('message', 'mime_xml', 'mime'), [
    (MESSAGE, None, None),
    (OLD_MESSAGE, XML_MIME, MIME_PARTS),
])
def test_copier_do_not_cache_fails(message, mime_xml, mime):
    with patched_mulcagate() as mg:
        mg.get_raw_data.side_effect = [storage.StorageGetError('')]
        mg.put.side_effect = ['new.Eve.stid']

        copier = STIDsCopier(MULCAGATE, USER_ID)
        assert copier.copy('orig.Alice.stid') == StidWithMime('orig.Alice.stid', None, None)

        mg.get_raw_data.side_effect = [message]
        assert copier.copy('orig.Alice.stid') == StidWithMime('new.Eve.stid', mime_xml, mime)
