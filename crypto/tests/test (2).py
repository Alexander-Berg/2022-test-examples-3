from __future__ import print_function

import mock
import pytest
import datetime

from crypta.graph.archivator.lib import BackupTask
from crypta.lib.python.yql_runner.tests import canonize_output, clean_up

import google.protobuf.text_format
import crypta.graph.archivator.proto.config_pb2 as proto_config

BACKUP_DIR = "//home/crypta/archive/graph/vertices_no_multi_profile"
SOURCE_PATH = (
    "//home/crypta/production/state/graph/v2/matching/vertices_no_multi_profile"
)


def get_default_backuper():
    backup = proto_config.TBackupCfg()
    backup.DirName = "daily"
    backup.Predicate = "lambda value: True"
    backup.Repeats = 370
    return [backup]


def get_protoconfig(difflog=False):
    text = """
        Yt {{
            Pool: "crypta_graph"
        }}

        Source: "{SOURCE_PATH}"
        BackupDir: "{BACKUP_DIR}"

        Backupers: [
            {{
                DirName: "daily"
                Predicate: "lambda date: bool(datetime.datetime.strptime(date, '%Y-%m-%d'))"
                Repeats: 60
            }},
            {{
                DirName: "weekly"
                Predicate: "lambda date: datetime.datetime.strptime(date, '%Y-%m-%d').weekday() == 4"
                Repeats: 7
                Master: "daily"
            }},
            {{
                DirName: "monthly"
                Predicate: "lambda date: datetime.datetime.strptime(date, '%Y-%m-%d').day == 1"
                PredicateGap: "lambda date, last: datetime.datetime.strptime(date, '%Y-%m-%d').month != datetime.datetime.strptime(last, '%Y-%m-%d').month"
                Repeats: 4
                Master: "daily"
            }},
            {{
                DirName: "difflog"
                DiffBy: ["id", "id_type"]
                VerifyField: "cryptaId"
                Repeats: 1
            }}
        ]
    """.format(
        SOURCE_PATH=SOURCE_PATH, BACKUP_DIR=BACKUP_DIR
    )

    proto = google.protobuf.text_format.Parse(text, proto_config.TArchivatorConfig())
    assert proto.Source == SOURCE_PATH
    assert proto.BackupDir == BACKUP_DIR
    if not difflog:
        del proto.Backupers[-1]
    return proto


BACKUPERS = get_protoconfig().Backupers


def load_tables(yt, date, data=None, dynamic=False):
    schema = [
        {"sort_order": "ascending", "type": "string", "required": False, "name": "id"},
        {
            "sort_order": "ascending",
            "type": "string",
            "required": False,
            "name": "id_type",
        },
        {"type": "string", "required": False, "name": "cryptaId"},
        {"type": "string", "required": False, "name": "merge_key"},
        {"type": "any", "required": False, "name": "merge_key_type"},
    ]

    yt.yt_client.create(
        "table",
        SOURCE_PATH,
        recursive=True,
        force=True,
        attributes={
            "generate_date": date,
            "schema": schema,
            "dynamic": dynamic,
        },
    )

    if data is None:
        data = {}

    if dynamic:
        yt.yt_client.mount_table(SOURCE_PATH, sync=True)
        yt.yt_client.insert_rows(SOURCE_PATH, data)
        yt.yt_client.unmount_table(SOURCE_PATH, sync=True)
    else:
        yt.yt_client.write_table(SOURCE_PATH, data)


def prepare_output(yt, root):
    if yt.yt_client.exists(root):
        return list(
            map(
                lambda tbl: (str(tbl), tbl.attributes.get("generate_date")),
                yt.yt_client.search(
                    root, node_type=["table", "link"], attributes=["generate_date"]
                ),
            )
        )
    return []


@pytest.mark.parametrize(
    "date",
    [
        "2020-09-01",  # day and month
        "2020-09-02",  # only day
        "2020-09-04",  # day and week
        "2020-05-01",  # day and week and month
    ],
)
@canonize_output
@clean_up(observed_paths=("//home/crypta",))
def test_run_backup(yt, date):
    """Check simple backup cases"""
    load_tables(yt, date)
    task = BackupTask(yt.yt_client)
    task.run(SOURCE_PATH, BACKUP_DIR, BACKUPERS)
    output = prepare_output(yt, BACKUP_DIR)
    assert 1 <= len(output) <= 3
    return output


@pytest.mark.parametrize(
    "date",
    [
        "2020-09-01",  # day and month
        "2020-09-02",  # only day (and month gap)
        "2020-09-04",  # day and week (and month gap)
        "2020-05-01",  # day and week and month
    ],
)
@canonize_output
@clean_up(observed_paths=("//home/crypta",))
def test_run_backup_with_month_gap(yt, date):
    """Check simple backup cases"""
    yt.yt_client.create(
        "table",
        "{}/month/2020-01-01".format(BACKUP_DIR),
        recursive=True,
        force=True,
        attributes={"generate_date": "2020-01-01"},
    )
    load_tables(yt, date)
    task = BackupTask(yt.yt_client)
    task.run(SOURCE_PATH, BACKUP_DIR, BACKUPERS)
    output = prepare_output(yt, BACKUP_DIR)
    assert 1 <= (len(output) - 1) <= 3
    return output


@canonize_output
@clean_up(observed_paths=("//home/crypta",))
def test_run_backup_twice(yt):
    """Check simple backup cases"""
    load_tables(yt, "2020-09-02")
    task = BackupTask(yt.yt_client)
    task.run(SOURCE_PATH, BACKUP_DIR, BACKUPERS)
    task.run(SOURCE_PATH, BACKUP_DIR, BACKUPERS)
    task.run(SOURCE_PATH, BACKUP_DIR, get_default_backuper())
    output = prepare_output(yt, BACKUP_DIR)
    assert len(output) == 1
    return output


@canonize_output
@clean_up(observed_paths=("//home/crypta",))
def test_run_backup_twice_move(yt):
    """Check simple backup cases"""
    load_tables(yt, "2020-09-02")
    task = BackupTask(yt.yt_client)
    task.run(SOURCE_PATH, BACKUP_DIR, BACKUPERS)
    yt.yt_client.set("{}/@generate_date".format(SOURCE_PATH), "2020-09-03")
    task.run(SOURCE_PATH, BACKUP_DIR, BACKUPERS)
    output = prepare_output(yt, BACKUP_DIR)
    assert len(output) == 2
    return output


@canonize_output
@clean_up(observed_paths=("//home/crypta",))
def test_run_backup_incorect_date_fail(yt):
    """Check wrong day case"""
    load_tables(yt, "2020-02-31")
    task = BackupTask(yt.yt_client)
    with pytest.raises(ValueError):
        task.run(SOURCE_PATH, BACKUP_DIR, BACKUPERS)
    output = prepare_output(yt, BACKUP_DIR)
    assert len(output) == 0
    return output


@canonize_output
@clean_up(observed_paths=("//home/crypta",))
def test_run_backup_cleanup(yt):
    """Check wrong clean up correctly"""
    load_tables(yt, "2020-09-01")
    # make previous backuped tables
    day = datetime.datetime(2020, 9, 1)
    for offset in range(366):
        date = (day - datetime.timedelta(days=offset)).strftime("%Y-%m-%d")
        for backup in BACKUPERS:
            path = "{0}/{1}/{2}".format(BACKUP_DIR, backup.DirName, date)
            yt.yt_client.create(
                "table", path, recursive=True, attributes={"generate_date": date}
            )

    task = BackupTask(yt.yt_client)
    task.run(SOURCE_PATH, BACKUP_DIR, BACKUPERS)

    output = prepare_output(yt, BACKUP_DIR)
    assert len(output) == 60 + 7 + 4
    return output


@canonize_output
@clean_up(observed_paths=("//home/crypta",))
def test_run_backup_cleanup_norun(yt):
    """Check wrong clean up correctly"""
    load_tables(yt, "2020-09-01")
    # make previous backuped tables
    day = datetime.datetime(2020, 9, 1)
    for offset in range(366):
        date = (day - datetime.timedelta(days=offset)).strftime("%Y-%m-%d")
        for backup in BACKUPERS:
            path = "{0}/{1}/{2}".format(BACKUP_DIR, backup.DirName, date)
            yt.yt_client.create(
                "table", path, recursive=True, attributes={"generate_date": date}
            )

    task = BackupTask(yt.yt_client)
    task.run(SOURCE_PATH, BACKUP_DIR, [])

    output = prepare_output(yt, BACKUP_DIR)
    assert len(output) == 366 * 3
    return output


@canonize_output
@clean_up(observed_paths=("//home/crypta",))
def test_run_backup_iterative(yt):
    """Run backup task for year and check backuped master"""
    day = datetime.datetime(2020, 9, 1)
    for offset in range(366):
        date = (day - datetime.timedelta(days=offset)).strftime("%Y-%m-%d")
        load_tables(yt, date)
        task = BackupTask(yt.yt_client)
        task.run(SOURCE_PATH, BACKUP_DIR, BACKUPERS)
    output = prepare_output(yt, BACKUP_DIR)
    assert len(output) == 60 + 7 + 4
    return output


def test_backupers_daily():
    """Check is day backupers correct"""
    backup_config = BACKUPERS[0]

    assert backup_config.DirName == "daily"
    assert backup_config.Repeats == 60
    assert not backup_config.Master
    day = datetime.datetime(2020, 1, 1)
    counter = 0
    for offset in range(366):
        date = day + datetime.timedelta(days=offset)
        # back each day
        assert eval(backup_config.Predicate)(date.strftime("%Y-%m-%d"))
        counter += int(eval(backup_config.Predicate)(date.strftime("%Y-%m-%d")))
    assert counter == 366


def test_backupers_weekly():
    """Check is week backupers correct"""
    backup_config = BACKUPERS[1]

    assert backup_config.DirName == "weekly"
    assert backup_config.Repeats == 7
    assert backup_config.Master == "daily"
    day = datetime.datetime(2020, 1, 1)
    counter = 0
    for offset in range(366):
        date = day + datetime.timedelta(days=offset)
        # back only on friday's
        assert eval(backup_config.Predicate)(date.strftime("%Y-%m-%d")) == (
            date.weekday() in {4}
        )
        counter += int(eval(backup_config.Predicate)(date.strftime("%Y-%m-%d")))
    assert counter == 52


def test_backupers_monthly():
    """Check is month backupers correct"""
    backup_config = BACKUPERS[2]

    assert backup_config.DirName == "monthly"
    assert backup_config.Repeats == 4
    assert backup_config.Master == "daily"
    day = datetime.datetime(2020, 1, 1)
    counter = 0
    for offset in range(366):
        date = day + datetime.timedelta(days=offset)
        # back only on mont's first day
        assert eval(backup_config.Predicate)(date.strftime("%Y-%m-%d")) == (
            date.day == 1
        )
        counter += int(eval(backup_config.Predicate)(date.strftime("%Y-%m-%d")))
    assert counter == 12


@canonize_output
@clean_up(observed_paths=("//home/crypta",))
def test_run_backup_diff(yt):
    """Check simple backup cases"""
    load_tables(
        yt,
        "2021-01-15",
        [
            {"id": "0y1", "id_type": "yuid", "cryptaId": "c0"},
            {"id": "1y2", "id_type": "yuid", "cryptaId": "c0"},
            {"id": "2y3", "id_type": "yuid", "cryptaId": "c1"},
            {"id": "3d1", "id_type": "gaid", "cryptaId": "c1"},
            {"id": "4p1", "id_type": "puid", "cryptaId": "c2"},
        ],
    )
    task = BackupTask(yt.yt_client)
    task.run(SOURCE_PATH, BACKUP_DIR, [get_protoconfig(difflog=True).Backupers[-1]])

    load_tables(
        yt,
        "2021-01-17",
        [
            {"id": "0y1", "id_type": "yuid", "cryptaId": "c0"},  # same will skip
            {"id": "1y2", "id_type": "yuid", "cryptaId": "c0"},  # same will skip
            # {"id": "2y3", "id_type": "yuid", "cryptaId": "c1"},  # remove vertex
            {"id": "3d1", "id_type": "gaid", "cryptaId": "c7"},  # change cid
            {"id": "4p1", "id_type": "puid", "cryptaId": "c0"},  # change cid
        ],
    )

    task.run(SOURCE_PATH, BACKUP_DIR, [get_protoconfig(difflog=True).Backupers[-1]])

    output = prepare_output(yt, BACKUP_DIR)
    assert len(output) == 3 + 1 + 3
    return {
        "t15": list(
            yt.yt_client.read_table(
                "//home/crypta/archive/graph/vertices_no_multi_profile/difflog/delta/2021-01-15",
                format="json",
            )
        ),
        "t17": list(
            yt.yt_client.read_table(
                "//home/crypta/archive/graph/vertices_no_multi_profile/difflog/delta/2021-01-17",
                format="json",
            )
        ),
        "tree": output,
    }


@pytest.mark.parametrize("dynamic", [False, True])
@canonize_output
@clean_up(observed_paths=("//home/crypta",))
def test_run_backup_dyn(yt, dynamic):
    """Check simple backup cases"""
    load_tables(
        yt,
        "2021-01-15",
        [
            {"id": "0y1", "id_type": "yuid", "cryptaId": "c0"},
            {"id": "1y2", "id_type": "yuid", "cryptaId": "c0"},
            {"id": "2y3", "id_type": "yuid", "cryptaId": "c1"},
            {"id": "3d1", "id_type": "gaid", "cryptaId": "c1"},
            {"id": "4p1", "id_type": "puid", "cryptaId": "c2"},
        ],
        dynamic,
    )
    task = BackupTask(yt.yt_client)
    task.run(SOURCE_PATH, BACKUP_DIR, [BACKUPERS[0]])

    output = prepare_output(yt, BACKUP_DIR)
    return {
        "t": list(
            yt.yt_client.read_table(
                "//home/crypta/archive/graph/vertices_no_multi_profile/daily/2021-01-15",
                format="json",
            )
        ),
        "tree": output,
    }


@canonize_output
@clean_up(observed_paths=("//home/crypta",))
def test_run_backup_diff_on_trim(yt):
    """Check simple backup cases"""
    load_tables(
        yt,
        "2021-01-13",
        [
            {"id": "0y1", "id_type": "yuid", "cryptaId": "c0"},
            {"id": "1y2", "id_type": "yuid", "cryptaId": "c0"},
            {"id": "2y3", "id_type": "yuid", "cryptaId": "c1"},
            {"id": "3d1", "id_type": "gaid", "cryptaId": "c1"},
            {"id": "4p1", "id_type": "puid", "cryptaId": "c2"},
        ],
    )
    task = BackupTask(yt.yt_client)
    task.run(SOURCE_PATH, BACKUP_DIR, [get_protoconfig(difflog=True).Backupers[-1]])

    load_tables(
        yt,
        "2021-02-14",
        [
            {"id": "0y1", "id_type": "yuid", "cryptaId": "c0"},  # same will skip
            {"id": "1y2", "id_type": "yuid", "cryptaId": "c0"},  # same will skip
            # {"id": "2y3", "id_type": "yuid", "cryptaId": "c1"},  # remove vertex
            {"id": "3d1", "id_type": "gaid", "cryptaId": "c7"},  # change cid
            {"id": "4p1", "id_type": "puid", "cryptaId": "c0"},  # change cid
        ],
    )

    task.run(SOURCE_PATH, BACKUP_DIR, [get_protoconfig(difflog=True).Backupers[-1]])

    output = prepare_output(yt, BACKUP_DIR)
    assert len(output) == 14 + 1 + 14
    return {
        "t14": list(
            yt.yt_client.read_table(
                "//home/crypta/archive/graph/vertices_no_multi_profile/difflog/delta/2021-02-14",
                format="json",
            )
        ),
        "tree": output,
    }


class FakeDateTime(datetime.datetime):

    """Fake date to mock immutable builtins"""

    @classmethod
    def now(cls):
        return cls(2021, 1, 2, 3, 40, 56)


@canonize_output
@clean_up(observed_paths=("//home/crypta",))
@mock.patch("datetime.datetime", FakeDateTime)
def test_run_backup_no_generate_date(yt):
    """Check simple backup cases"""
    load_tables(yt, "2020-09-02")
    yt.yt_client.remove("{path}/@generate_date".format(path=SOURCE_PATH))
    task = BackupTask(yt.yt_client)
    task.run(SOURCE_PATH, BACKUP_DIR, BACKUPERS)
    output = prepare_output(yt, BACKUP_DIR)
    assert len(output) == 1
    return output
