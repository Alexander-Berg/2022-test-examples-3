

import os
import pytest
import yatest.common


from lib import instance


def get_root():
    return yatest.common.test_source_path('test_get_volume_path')


@pytest.mark.parametrize(
    'uuid,result',
    [
        ('', None),
        ('234', None),
        ('123', os.path.join(get_root(), 'place/db/iss3/volumes/123')),
    ],
    ids=['no_uuid_no_path', 'bad_uuid_no_path', 'good_uuid_good_path']
)
def test_get_volume_path(uuid, result, requests_mock):
    iss_config_url = 'http://localhost:25536'

    file_data = os.path.join(get_root(), 'db/iss3/application.conf')

    with open(file_data, 'r') as f:
        mock_data = f.read()

    requests_mock.get('%s/config' % iss_config_url, text=mock_data)

    test_instance = instance.Instance(path='', volume_uuid=uuid, conf_id='', tags='')
    path = test_instance.get_volume_path(get_root(), iss_config_url)
    assert path == result
