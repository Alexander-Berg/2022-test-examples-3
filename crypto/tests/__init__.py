from __future__ import print_function

import datetime
import inspect
import json
import time
import uuid
import yatest.common

import mock  # noqa

from decorator import decorator
from library.python import resource
from six.moves import range

from crypta.lib.python.bt.workflow import execute_sync
from crypta.lib.python.zk import fake_zk_client

import yt.wrapper as yt_wrapper

yt_wrapper.format.JSON_ENCODING_LEGACY_MODE = True


def read_resource(fname, by_rows=True):
    if fname.endswith(".json"):
        if by_rows:
            lines = resource.find(fname)
            if lines is None:
                raise Exception("Resource is not found %s" % fname)
            for line in lines.splitlines():
                yield json.loads(line)
        else:
            yield json.loads(resource.find(fname))
    elif fname.endswith(".yson"):
        for line in yt_wrapper.yson.loads(resource.find(fname)):
            yield line


class ForceInferSchema(object):

    """ Load data into YT with schema """

    def __init__(self, table_path, fixture, spec=None):
        self.table_path = table_path
        self.fixture = fixture
        self.spec = spec

    def load_data(self, yt):
        data = list(read_resource(self.fixture))
        if self.spec is not None:
            attributes = next(read_resource(self.spec, by_rows=False))
        else:
            attributes = {"schema": self.infer_schema(data, self.table_path)}
        # print("Write data into table {tbl} with attributes {attrib}".format(
        #     tbl=self.table_path, attrib=attributes), file=sys.stderr)
        yt.yt_client.create("table", self.table_path, recursive=True, attributes=attributes)
        yt.yt_client.write_table(self.table_path, data, format="json")

    def infer_schema(self, data, table):
        """ Read all table rows and accomulate key, value pairs to make maximum full shema """
        record = {}
        for item in data:
            for key, value in item.items():
                if key in record and value is None:
                    continue
                record[key] = value
        return [{"name": key, "type": self._infer_type(key, value, table)} for key, value in record.items()]

    def _infer_type(self, key, value, path):  # C901 # noqa
        """ is not realy correct, but required schema with Uint64
        _logfeller_timestamp and yt make Int64 type by default """
        if path.endswith("all_radius_ips") and key == "ip":
            # (False and path.endswith('rtb_log_apps') and key == 'device_id')
            # rtb log has Yson as device_id field
            # radius ips log has Yson as ip field
            return "any"
        if ("households_new/st" in path) and (key in ("yuid", "hits")):
            # households storage byteebstvo integer
            return "uint64"
        if key == "_logfeller_timestamp":
            return "uint64"
        if isinstance(value, bool):
            return "boolean"
        if isinstance(value, int):
            return "int64"
        if isinstance(value, float):
            return "double"
        if isinstance(value, (dict, list)):
            return "any"
        return "string"


def _get_yt_from_args(function, *args, **kwargs):
    if "yt" in kwargs:
        return kwargs["yt"]
    elif "local_yt" in kwargs:
        return kwargs["local_yt"]
    else:
        args_spec = inspect.getargspec(function).args
        if "local_yt" in args_spec:
            return args[args_spec.index("local_yt")]
        return args[args_spec.index("yt")]


def _load_fixtures(yt, fixtures):
    print("FIXTURES {}".format(str(fixtures)))
    assert isinstance(yt.yt_proxy_port, int)
    if len(fixtures) and callable(fixtures[0]):
        # call loadre implementation
        fixtures[0](yt)
        return
    # load from attached tables
    for table in fixtures:
        ForceInferSchema(*table).load_data(yt)


def load_fixtures(*fixtures):
    def wrapper(function, *args, **kwargs):
        yt = _get_yt_from_args(function, *args, **kwargs)
        _load_fixtures(yt, fixtures)
        return function(*args, **kwargs)

    return decorator(wrapper)


@decorator
def canonize_output(function, *args, **kwargs):
    canon = function(*args, **kwargs)
    file_path = "results.{}.json".format(function.__name__)
    with open(file_path, "w") as out_file:
        json.dump(canon, out_file, sort_keys=True, indent=4)

    return [yatest.common.canonical_file(file_path)]


@decorator
def clean_up(function, observed_paths=None, *args, **kwargs):
    """ Remove all data from yt after test """
    yt = _get_yt_from_args(function, *args, **kwargs)
    results = None

    def _clean_up():
        """ clean up after function """
        for path in observed_paths or ("//home",):
            try:
                yt.yt_client.remove(path, recursive=True)
            except:
                pass

    try:
        results = function(*args, **kwargs)
    except:
        _clean_up()
        raise
    else:
        _clean_up()
        return results


def execute(task):
    with fake_zk_client() as fake_zk:
        execute_sync(task, fake_zk, do_fork=False)


def unmount(yt, table):
    yt.unmount_table(table)
    for attempt in range(10):
        time.sleep(3 + attempt)
        if yt.get("{0}/@tablets/0/state".format(table)) == "unmounted":
            return


class FakeDate(datetime.date):

    """ Fake date to mock immutable builtins """

    @classmethod
    def today(cls):
        return cls(2020, 11, 1)


class FakeDateTime(datetime.datetime):

    """ Fake date to mock immutable builtins """

    @classmethod
    def now(cls):
        return cls(2020, 11, 1, 12, 0, 0)


@mock.patch.dict("os.environ", {"YT_TOKEN": "FAKE", "ENV_TYPE": "FAKE"})
@mock.patch("time.time", mock.MagicMock(return_value=1561460704.213396))
@mock.patch(
    "crypta.graph.data_import.stream.lib.tasks.base.get_table_date",
    mock.MagicMock(return_value=datetime.datetime.now()),
)
@mock.patch("uuid.uuid4", mock.MagicMock(return_value=uuid.UUID("7f8add24-a917-485a-a49a-9e55dce4d70d")))
@mock.patch("solomon.solomon.ThrottledPushApiReporter._push", mock.MagicMock(return_value=True))
def do_stream_test(task_class, local_yt, conf):
    task = task_class()
    execute(task)

    def select_all(table):
        return sorted(list(local_yt.yt_client.read_table(table, format="json")))

    def select_root(root):
        tables = sorted(local_yt.yt_client.search(root, node_type="table"))
        return {table: select_all(table) for table in tables}

    unmount(local_yt.yt_client, conf.paths.stream.processed)

    res = {
        "data": select_all(list(task.output())[0]),
        "attributes": local_yt.yt_client.get("{path}/@processed".format(path=list(task.output())[0])),
        "processed": sorted(select_all(conf.paths.stream.processed)),
    }

    extra_data = (("idstorage", conf.paths.stream.id_storage), ("extra_data", conf.paths.stream.extra_data))

    for (key, path) in extra_data:
        if local_yt.yt_client.exists(path):
            data = select_root(path)
            if data:
                res[key] = data

    return res
