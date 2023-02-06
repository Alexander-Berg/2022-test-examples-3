from crypta.cm.services.common.changes.python.touch_command import TTouchCommand
from crypta.cm.services.common.data.python.id import TId


def test_to_string():
    ext_id = TId("type", "value")
    touch_ts = 1500000000
    timestamp = 1500000001

    touch_command = TTouchCommand(ext_id.Value, ext_id, touch_ts, timestamp)
    serialized_command = TTouchCommand.ToString(touch_command)

    ref = '{{"cmd":"touch","ext_id":"{type}:{value}","touch_ts":{touch_ts},"unixtime":{timestamp},"sharding_key":"{sharding_key}"}}'.format(
        type=ext_id.Type, value=ext_id.Value, touch_ts=touch_ts, timestamp=timestamp, sharding_key=ext_id.Value,
    )
    assert ref == serialized_command
