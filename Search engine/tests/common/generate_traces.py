import string
import uuid
import random

from search.martylib.core.date_utils import now
from search.martylib.proto.structures.trace_pb2 import ParserContext as ctx

from search.sawmill.proto import trace_pb2


WEEK = 7 * 24 * 60 * 60
COMMON_TTL = [WEEK, 2 * WEEK, 40 * WEEK]
COMMON_TIMEOUTS = [15, 30, 60, 120, 300, 600]
EXISTING_STATUSES = ['ENQUEUED', 'IN_PROGRESS', 'COMPLETE', 'FAILED']
EXISTING_SERVICES = ['__upload__', 'zephyr', 'beholder', 'findurl', 'yappy', 'yappy-dev', 'sawmill', 'horizon-resolvers']


def generate_stored_traces(cnt=-1):
    while cnt != 0:
        cnt -= 1
        yield random_stored_trace()


def random_stored_trace():
    return trace_pb2.StoredTrace(
        id=str(uuid.uuid1()),
        request=random_trace_start_request(),
        status=random.choice(EXISTING_STATUSES),
        added=round(now().timestamp(), 3),
        started=round(now().timestamp() + random.random(), 3),
        ended=round(now().timestamp() + random.random(), 3),
        frame_count=random.randint(0, 10000),
        uploaded=random.choice([True, False]),
        ttl=random.choice(COMMON_TTL),
    )


def random_alphanumeric(size, collection=string.ascii_lowercase + string.digits):
    return ''.join(random.choices(collection, k=size))


def random_trace_start_request():
    return trace_pb2.TraceStartRequest(
        service_id=random.choice(EXISTING_SERVICES),
        ttl=random.choice(COMMON_TTL),
        timeout=random.choice(COMMON_TIMEOUTS),
        parser_context=random_parser_context(),
    )


def generate_upload_request(cnt=-1):
    while cnt != 0:
        cnt -= 1
        yield random_upload_request()


def random_upload_request():
    return trace_pb2.UploadRequest(
        parser_context=random_parser_context(),
        fqdn=random_alphanumeric(30),
    )


def random_parser_context():
    return ctx(
        max_emitted_frames=random.randint(0, 1000),
        max_flushed_frames=random.randint(0, 1000),
        max_consumed_lines=random.randint(0, 1000),
        offset=random.randint(1, 1000),
        timeout=round(random.random(), 3),
    )
