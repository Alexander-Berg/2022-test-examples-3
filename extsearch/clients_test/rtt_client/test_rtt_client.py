from pytest import mark

from extsearch.video.ugc.sqs_moderation.clients.rtt.rtt_client import RttResponseError
from requests import HTTPError


def test_rtt_create_task_ok(rtt_client, new_task_data_ok):
    resp = rtt_client.create_task(new_task_data_ok)
    assert resp.get('task_id') is not None


def test_rtt_get_task_ok(rtt_client, test_task_id):
    resp = rtt_client.get_task(test_task_id)
    assert resp.get('task_id') == test_task_id


http_err_data = [
    400,
    500,
]


@mark.parametrize("code", http_err_data)
def test_rtt_get_task_http_err(rtt_client, code):
    try:
        rtt_client.get_task(f'http_err_{code}')
        assert False, 'Error not raised'
    except HTTPError:
        return
    except Exception as e:
        assert False, f'Wrong error raised {e}'


@mark.parametrize("code", http_err_data)
def test_rtt_create_task_http_err(rtt_client, code):
    try:
        rtt_client.create_task({'http_err': code})
        assert False, 'Error not raised'
    except HTTPError:
        return
    except Exception as e:
        assert False, f'Wrong error raised {e}'


transcoder_data = [
    {'err_code': 2, 'expected': {}},
]


def test_rtt_create_task_transcoder_error(rtt_client, new_task_err_2):
    try:
        rtt_client.create_task(new_task_err_2)
        assert False, 'Error not raised'
    except RttResponseError:
        return
    except Exception as e:
        assert False, f'Wrong error raised {e}'
