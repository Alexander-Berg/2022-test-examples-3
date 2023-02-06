from future.standard_library import install_aliases
install_aliases()

from urllib.error import HTTPError

from contextlib import contextmanager
from io import BytesIO as SIO
from ora2pg import storage
from ora2pg.storage import MulcaGate
import mock
import pytest

# pylint: disable=R0201


@contextmanager
def patch_response(response):
    yield SIO(response)


@contextmanager
def patch_request():
    with mock.patch(
        'ora2pg.storage.request',
    ) as mocked_req:
        with mocked_req.return_value as fd:
            fd.read.return_value = b'some_stid'
        yield mocked_req


def make_http_error(code):
    return HTTPError(
        url='test://mulcagate:10010',
        code=code,
        msg='some test http error',
        hdrs=[],
        fp=SIO(b'some error file')
    )


class SomeIOError(IOError):
    pass


def test_has_xml_part_for_message_with_xml_part():
    assert storage.has_xml_part(b'<?xml version="1.0"?>\n<message>xml_data</message>\nMessage')


def test_has_xml_part_for_message_without_xml_part():
    assert not storage.has_xml_part(b'<message>xml_data</message>\nMessage')


def test_has_xml_part_for_xml_in_message():
    assert not storage.has_xml_part(b'Message<?xml version="1.0"?>\n<message>xml_data</message>\n')


def test_split_mime_xml_and_message_for_data_with_xml_part():
    mime_xml, message = storage.split_mime_xml_and_message(b'<?xml version="1.0"?>\n<message>xml_data</message>\nMessage')
    assert mime_xml == b'<?xml version="1.0"?>\n<message>xml_data</message>\n'
    assert message == b'Message'


def test_split_mime_xml_and_message_for_data_without_xml_part():
    mime_xml, message = storage.split_mime_xml_and_message(b'<message>xml_data</message>\nMessage')
    assert mime_xml is None
    assert message == b'<message>xml_data</message>\nMessage'


class TestStorageGetRawData(object):

    def test_add_raw_and_service_to_query_args(self):
        with patch_request() as mocked_request:
            MulcaGate('test://mulcagate').get_raw_data('ST_ID')

            mocked_request.assert_called_once_with(
                'test://mulcagate:10010/gate/get/ST_ID?raw=&service=maildbtransfer',
                do_retries=True,
                headers={},
                skip_retry_codes=[404],
                cafile=None,
            )

    def test_save_xml_part_in_response(self):
        with patch_request() as mocked_request:
            response = b'<?xml version="1.0" encoding="utf-8"?>\n<message>message_xml_data</message>\nMessage'
            mocked_request.return_value = patch_response(response)
            assert MulcaGate('test://mulcagate').get_raw_data('ST_ID') == response

            mocked_request.assert_called_once_with(
                'test://mulcagate:10010/gate/get/ST_ID?raw=&service=maildbtransfer',
                do_retries=True,
                headers={},
                skip_retry_codes=[404],
                cafile=None,
            )

    def test_throw_deleted_message_on_404(self):
        with patch_request() as mocked_request:
            mocked_request.side_effect = [make_http_error(404)]
            with pytest.raises(storage.DeletedMessage):
                MulcaGate('test://mulcagate').get_raw_data('DELETED_ST_ID')

    @pytest.mark.parametrize('error', [
        make_http_error(500),
        SomeIOError(''),
    ])
    def test_raise_mulca_get_error_on_http_or_io_errors(self, error):
        with patch_request() as mocked_request:
            mocked_request.side_effect = [error]
            with pytest.raises(storage.StorageGetError):
                MulcaGate('test://mulcagate').get_raw_data('UNHAPPY_ST_ID')


class TestStorageGet(object):

    def test_add_raw_and_service_to_query_args(self):
        with patch_request() as mocked_request:
            storage.MulcaGate('test://mulcagate').get('ST_ID')
            mocked_request.assert_called_once_with(
                'test://mulcagate:10010/gate/get/ST_ID?raw=&service=maildbtransfer',
                do_retries=True,
                headers={},
                skip_retry_codes=[404],
                cafile=None,
            )

    def test_erase_xml_part_from_response(self):
        with patch_request() as mocked_request:
            mocked_request.return_value = patch_response(
                b'<?xml version="1.0" encoding="utf-8"?>\n<message>message_xml_data</message>\nMessage'
            )
            assert storage.MulcaGate('test://mulcagate').get('ST_ID')== b'Message'

            mocked_request.assert_called_once_with(
                'test://mulcagate:10010/gate/get/ST_ID?raw=&service=maildbtransfer',
                do_retries=True,
                headers={},
                skip_retry_codes=[404],
                cafile=None,
            )

    def test_save_message_tag_not_xml_part_in_response(self):
        with patch_request() as mocked_request:
            response = b'<message>some_text</message>\nMessage'
            mocked_request.return_value = patch_response(response)
            assert MulcaGate('test://mulcagate').get('ST_ID') == response

            mocked_request.assert_called_once_with(
                'test://mulcagate:10010/gate/get/ST_ID?raw=&service=maildbtransfer',
                do_retries=True,
                headers={},
                skip_retry_codes=[404],
                cafile=None
            )

    def test_throw_deleted_message_on_404(self):
        with patch_request() as mocked_request:
            mocked_request.side_effect = [make_http_error(404)]
            with pytest.raises(storage.DeletedMessage):
                MulcaGate('test://mulcagate').get('DELETED_ST_ID')

    @pytest.mark.parametrize('error', [
        make_http_error(500),
        SomeIOError(''),
    ])
    def test_raise_mulca_get_error_on_http_or_io_errors(self, error):
        with patch_request() as mocked_request:
            mocked_request.side_effect = [error]
            with pytest.raises(storage.StorageGetError):
                MulcaGate('test://mulcagate').get('UNHAPPY_ST_ID')


class TestStoragePut(object):

    def test_with_default_params(self):
        with patch_request() as mocked_request:
            MulcaGate('test://mulcagate').put(base_id='Eve', data='Love Wally')

            mocked_request.assert_called_once_with(
                'test://mulcagate:10010/gate/put/Eve?elliptics=1&ns=mail&raw=&service=maildbtransfer',
                data='Love Wally',
                do_retries=True,
                headers={},
                timeout=30,
                log_post_data=False,
                cafile=None,
            )

    def test_with_storage_namespace_params(self):
        with patch_request() as mocked_request:
            MulcaGate('test://mulcagate', mg_namespace='test-namespace').put(
                base_id='Eve',
                data='Love Wally',
            )

            mocked_request.assert_called_once_with(
                'test://mulcagate:10010/gate/put/Eve?elliptics=1&ns=test-namespace&raw=&service=maildbtransfer',
                data='Love Wally',
                do_retries=True,
                headers={},
                timeout=30,
                log_post_data=False,
                cafile=None,
            )

    def test_put_to_mds_with_mime_xml(self):
        with patch_request() as mocked_request:
            with pytest.raises(storage.StoragePutError):
                MulcaGate('test://mulcagate').put(
                    base_id='Eve',
                    data='<?xml version="1.0"?>\n<message>message_xml_data</message>\nMessage'
                )
            assert not mocked_request.called

    @pytest.mark.parametrize('error', [
        make_http_error(500),
        SomeIOError('')
    ])
    def test_raise_mulca_put_on_errors(self, error):
        with patch_request() as mocked_request:
            mocked_request.side_effect = [error]
            with pytest.raises(storage.StoragePutError):
                MulcaGate('test://mulcagate').put('Eve', 'Love Wally')


class TestStorageDelete(object):

    def test_add_service_to_query_args(self):
        with patch_request() as mocked_request:
            MulcaGate('test://mulcagate').delete('ST_ID')

            mocked_request.assert_called_once_with(
                'test://mulcagate:10010/gate/del/ST_ID?service=maildbtransfer',
                do_retries=True,
                headers={},
                cafile=None,
            )

    @pytest.mark.parametrize('error', [
        make_http_error(500),
        SomeIOError(''),
    ])
    def test_raise_storage_del_error_on_http_or_io_errors(self, error):
        with patch_request() as mocked_request:
            mocked_request.side_effect = [error]
            with pytest.raises(storage.StorageDelError):
                MulcaGate('test://mulcagate').delete('UNHAPPY_ST_ID')
