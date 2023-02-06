from pytest import mark

from load.projects.cloud.tank_client.send_logs import split_data


@mark.parametrize(('data', 'slice_len', 'exp_result'), [
    ('My way', 3, ['My ', 'way']),
    ('My way', 4, ['My w', 'ay']),
    ('My way!', 2, ['My', ' w', 'ay', '!']),
    ('My way', 20, ['My way']),
])
def test_split_data(data, slice_len, exp_result):
    result = split_data(data, slice_len)
    assert result == exp_result
