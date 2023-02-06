# encoding: utf-8

import time
import travel.proto.commons_pb2 as commons_pb2

yt_schema = [
    {'name': 'MessageId', 'type': 'string', 'sort_order': 'ascending'},
    {'name': 'Timestamp', 'type': 'uint64'},
    {'name': 'ExpireTimestamp', 'type': 'uint64'},
    {'name': 'MessageType', 'type': 'string'},
    {'name': 'Codec', 'type': 'uint64'},
    {'name': 'Bytes', 'type': 'string'},
]


class InitialMessage(object):
    def __init__(self, message_id, data):
        self.message_id = message_id
        self.data = data


class YtKeyValueStorage(object):
    def __init__(self, yt_stuff, table_path, now=int(time.time() * 1000)):
        self.yt_client = yt_stuff.yt_client
        self.server_name = yt_stuff.get_server()
        self.table_path = table_path
        self.now = now
        self.read_row_index = 0

        self.yt_client.create('table', self.table_path, attributes={'schema': yt_schema, 'dynamic': True, 'max_data_ttl': 1 * 60 * 60 * 1000}, recursive=True)
        self.yt_client.mount_table(self.table_path, sync=True)

    def write(self, message_type, messages):
        rows = [{
            'Timestamp': self.now,
            'MessageType': message_type,
            'Codec': commons_pb2.MC_NONE,
            'Bytes': message.data.SerializeToString(),
            'MessageId': message.message_id,
            'ExpireTimestamp': self.now + 1200 * 1000,
        } for message in messages]
        self.yt_client.insert_rows(self.table_path, rows)
