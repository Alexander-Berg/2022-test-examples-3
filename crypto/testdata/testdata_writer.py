import json
from collections import defaultdict
import data_set
import allure
import pytest

import testdata_helper

MASK_FOR_DETECT_TEST_DATA = "testdata_"


def select_test_data(source):
    return [source.__dict__.get(name) for name in dir(source) if name and MASK_FOR_DETECT_TEST_DATA in name]


def prepare_dirs(yt, dt, registry):
    # TODO: why can't we create dirs from test data
    yt.mkdir('//home/freshness/crypta', recursive=True)
    yt.mkdir('//crypta/production/state/iscrypta', recursive=True)
    yt.mkdir('//crypta/production/state/webvisor_date_processed', recursive=True)
    yt.mkdir('//home/logfeller/logs/mobreport-access-log/1d', recursive=True)
    yt.mkdir('//home/logfeller/logs/sbapi-access-mitb-log/1d', recursive=True)
    yt.mkdir('//crypta/production/state/graph/dicts/passport', recursive=True)
    yt.mkdir('//crypta/production/state/graph/dicts/yamoney', recursive=True)
    yt.mkdir('//statbox/cube/data/mobile_install', recursive=True)
    yt.mkdir('//statbox/extdata-apps-flyer-log', recursive=True)
    yt.mkdir('//home/logfeller/logs/metrika-postclicks-log/1d', recursive=True)
    yt.mkdir('//statbox/bs-chevent-log/1d', recursive=True)
    yt.mkdir("//crypta/production/state/extras/reference-bases", recursive=True)
    yt.mkdir("//crypta/production/lal_manager", recursive=True)

    yt.mkdir('//crypta/production/state/graph/v2/soup/day', recursive=True)
    yt.mkdir('//crypta/production/state/graph/v2/soup/day/tmp', recursive=True)
    yt.mkdir('//crypta/production/state/graph/v2/soup/dumps', recursive=True)

    yt.mkdir("//home/crypta/analytics/segments/external_data/instagram-puid", recursive=True)

    yt.mkdir("//crypta/production/classification/exact_socdem_storage", recursive=True)

    yt.mkdir('//crypta/production/state/household', recursive=True)

    for log in registry:
        table_names = log.get_log().keys()
        for tbl in table_names:
            path = '/'.join(tbl.split('/')[:-1])
            if not yt.exists(path):
                yt.mkdir(path, recursive=True)


def get_type(key, value, path):
    """ Simple schematize choices """
    if any(((path.endswith('rtb_log_apps') and key == 'device_id'),
            (path.endswith('all_radius_ips') and key == 'ip'), )):
        # rtb log has Yson as device_id field
        # radius ips log has Yson as ip field
        return 'any'
    if key == '_logfeller_timestamp':
        # Error: incompatible WeakField types: Uint64?!=Int64?
        return 'uint64'
    if type(value) in (bool, ):
        return 'boolean'
    if isinstance(value, int):
        return 'int64'
    if isinstance(value, (dict, list)):
        return 'any'
    return 'string'

def get_sort(key, value, path):
    """
    Insert order into schema for sorted table
    to fix error with 'requirement expectedSortedBy == realSortedBy failed'
    """
    if value == 'yuid' and path.endswith('/yuid_with_all'):
        return True
    return False


def force_infer_schema(data, path):
    """ Read all table rows and accomulate key, value pairs to make maximum full shema """
    record = {}
    for item in data:
        record.update(item)
    schema = []
    for key, value in record.items():
        type_ = get_type(key, value, path)
        sort = get_sort(key, value, path)
        record = {'name': key, 'type': type_}
        if sort:
            record['sort_order'] = 'ascending'
        schema.append(record)
    return schema


def create_log(yt, dt):
    prepare_dirs(yt, dt, testdata_helper.BaseLog.registry)

    test_data = select_test_data(data_set)

    with pytest.allure.step('Test data created'):
        for d in test_data:
            one_log_data = defaultdict(list)
            for k, v in d.get_log().iteritems():
                one_log_data[k].extend(v)

            for path, rows in one_log_data.iteritems():
                allure.attach(str(path), json.dumps(rows, sort_keys=True, indent=4))
                if any((key in path for key in (
                        'statbox', 'logfeller', 'all_radius_ips', 'dicts/devid_hash', 'dicts/yuid_with_all', ))):
                    # make schema for logs
                    yt.create('table', yt.TablePath(
                        path, append=d.append), recursive=True, attributes={'schema': force_infer_schema(rows, path)})
                yt.write_table(yt.TablePath(path, append=d.append), [record for record in rows], raw=False)

        # partners data
        for log in testdata_helper.BaseLog.registry:
            if isinstance(log, testdata_helper.SingleTableLog):
                # create folders for single log tables
                yt.mkdir(log.folder_path, recursive=True)

                one_log_data = defaultdict(list)

                for k, v in log.get_log().iteritems():
                    one_log_data[k].extend(v)

                for path, rows in one_log_data.iteritems():
                    allure.attach(
                        "Filling YT table {} with {} records".format(str(log.folder_path), len(rows)),
                        json.dumps(rows, sort_keys=True, indent=4)
                    )
                    yt.write_table(
                        yt.TablePath(path, append=log.append),
                        [record for record in rows],
                        raw=False
                    )
