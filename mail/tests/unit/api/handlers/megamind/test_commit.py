import pytest

from mail.ciao.ciao.api.handlers.megamind.commit import CommitMegamindHandler


@pytest.mark.parametrize('data,frames_data', (
    ({}, []),
    ({'arguments': {}}, []),
    ({'arguments': {'semantic_frames': []}}, []),
    (
        {
            'arguments': {
                'semantic_frames': [{'x': 'y'}, {'x2': 'y2'}],
            }
        },
        [{'x': 'y'}, {'x2': 'y2'}],
    ),
))
def test_get_frames_data(data, frames_data):
    assert CommitMegamindHandler.get_frames_data(data) == frames_data


def test_make_response_data_success(mocker, rands):
    response = mocker.Mock()
    response.error = None
    assert CommitMegamindHandler.make_response_data(response, rands(), rands()) == {'success': {}}


def test_make_response_data_error(mocker, rands):
    response = mocker.Mock()
    message = response.error = 'some value'
    assert CommitMegamindHandler.make_response_data(response, rands(), rands()) == {
        'error': {'message': message}
    }
