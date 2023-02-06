# encoding: utf-8

import time
from uuid import uuid4
import travel.proto.commons_pb2 as commons_pb2


# Для запуска тестов на локальном ноуте надо прописать '::1 localhost' в /etc/hosts
yt_config_args = dict(wait_tablet_cell_initialization=True)


yt_schema = [
    {'name': 'Timestamp', 'type': 'uint64'},
    {'name': 'MessageType', 'type': 'string'},
    {'name': 'Codec', 'type': 'uint64'},
    {'name': 'Bytes', 'type': 'string'},
    {'name': 'MessageId', 'type': 'string'},
    {'name': 'ExpireTimestamp', 'type': 'uint64'},
]


class MessageBus(object):
    def __init__(self, yt_stuff, table_path, now=int(time.time() * 1000)):
        self.yt_client = yt_stuff.yt_client
        self.server_name = yt_stuff.get_server()
        self.table_path = table_path
        self.now = now
        self.read_row_index = 0

        self.yt_client.create('table', self.table_path, attributes={'schema': yt_schema, 'dynamic': True, 'max_data_ttl': 1 * 60 * 60 * 1000}, recursive=True)
        self.yt_client.mount_table(self.table_path, sync=True)

    def read(self, message_type, message_proto_factory):
        messages = []
        for row in self.yt_client.select_rows('[$row_index], Bytes from [{}] WHERE [$row_index] >= {} AND MessageType = "{}"'
                                              .format(self.table_path, self.read_row_index, message_type)):
            pb = message_proto_factory()
            pb.ParseFromString(row['Bytes'])
            self.read_row_index = row['$row_index'] + 1
            messages.append(pb)
        return messages

    def write(self, message_type, messages):
        rows = [{
            'Timestamp': self.now,
            'MessageType': message_type,
            'Codec': commons_pb2.MC_NONE,
            'Bytes': message.SerializeToString(),
            'MessageId': str(uuid4()),
            'ExpireTimestamp': self.now + 1200 * 1000,
        } for message in messages]
        self.yt_client.insert_rows(self.table_path, rows)
