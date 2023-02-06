import datetime
from yql.api.v1.client import YqlClient
from mail.xiva.crm.src.user_events_db import UserEventsDBYT
from mail.xiva.crm.src.util import YtPaths


def date_str(days=0):
    return (datetime.datetime.now() + datetime.timedelta(days=days)).strftime("%Y-%m-%d")


def time_str():
    return "T00:00::00"


def prepare_metrika_tables(rows, client, yt_paths):
    column_names = [
        'EventID', 'UUID', 'DeviceID', 'DeviceIDHash', 'AccountID', 'ReceiveDate', 'RegionID',
        'RegionTimeZone', 'timestamp', 'ParsedParams_Key1', 'ParsedParams_Key2', 'OperatingSystem',
        'OriginalManufacturer', 'OriginalModel', 'OSVersion', 'OSApiLevel', 'Locale', 'DeviceType',
        'AppID', 'AppPlatform', 'AppVersionName', 'AppBuildNumber', 'ReceiveTimestamp', 'APIKey',
        'EventName', 'EventType', 'ReportEnvironment_Keys', 'ReportEnvironment_Values']
    column_types = ['String?' if column == 'AccountID' else 'String' for column in column_names]
    tables = [
        "{}/{}".format(yt_paths.METRIKA_5M, time_str()),
        "{}/{}".format(yt_paths.METRIKA_1D, date_str(-1)),
        "{}/{}".format(yt_paths.METRIKA_1D, date_str(-2))
    ]
    for i in range(3):
        data = [[row[column] if column in row else '' for column in column_names] for row in rows[i]]
        client.write_table(tables[i], data, column_names, column_types)


def prepare_xeno_tables(rows, client, yt_paths):
    column_names = ['uid']
    column_types = ['String']
    client.write_table("{}/{}".format(yt_paths.XENO_1D, date_str(-1)), [], column_names, column_types)
    client.write_table("{}/{}".format(yt_paths.XENO_5M, time_str()), rows, column_names, column_types)


def prepare_devices_table(client, yt_paths):
    column_names = [
        'platform', 'os_api_level', 'device_id', 'uuid', 'region_id', 'os_version', 'os',
        'app_version', 'manufacturer', 'app_build', 'locale', 'timezone', 'model', 'device_type',
        'app_id']
    types = {
        'timezone': 'Int32',
        'locale': 'String',
        'device_id': 'String'
    }
    column_types = [types[column] if column in types else 'String?' for column in column_names]
    client.write_table(yt_paths.DEVICES, [], column_names, column_types)


def prepare_users_table(client, yt_paths):
    column_names = ['uid', 'mining_ts']
    column_types = ['String', 'Timestamp']
    client.write_table(yt_paths.USERS, [], column_names, column_types)


def prepare_user_device_table(client, yt_paths):
    column_names = ['uid', 'device_id']
    column_types = ['String', 'String']
    client.write_table(yt_paths.USER_DEVICE, [], column_names, column_types)


def prepare_tables(client, yt_paths):
    prepare_devices_table(client, yt_paths)
    prepare_users_table(client, yt_paths)
    prepare_user_device_table(client, yt_paths)


def test_user_events_db():
    client = YqlClient(db='hahn', token_path="/home/kharybin/.yql/token")
    yt_paths = YtPaths({
        'root': 'home/xiva/crm-dev',
        'xeno_root': 'home/xiva/crm-dev/xeno-log',
        'metrika_root': 'home/xiva/crm-dev/metrika-log',
    })
    metrika_rows = [
        [
            {
                'EventID': '1000',
                'DeviceID': 'signature_device_id',
                'AccountID': 'signature_uid',
                'EventName': 'settings-tap-signature',
                'APIKey': '29733',
                'AppPlatform': 'iOs'
            },
            {
                'EventID': '1001',
                'DeviceID': 'dark_theme_device_id',
                'AccountID': 'dark_theme_uid',
                'EventName': 'settings_dark_theme_turn_on',
                'APIKey': '29733',
                'AppPlatform': 'iOs'
            }],
        [
            {
                'EventID': '2000',
                'DeviceID': 'new_user_device_id',
                'AccountID': 'new_user_uid',
                'EventType': 'EVENT_INIT',
                'APIKey': '29733',
                'RegionTimeZone': '10800',
                'AppPlatform': 'iOs'
            },
            {
                'EventID': '2001',
                'DeviceID': 'not_new_user_device_id',
                'AccountID': 'not_new_user_uid',
                'EventType': 'EVENT_INIT',
                'APIKey': '29733',
                'RegionTimeZone': '10800',
                'AppPlatform': 'iOs'
            },
            {
                'EventID': '2002',
                'DeviceID': 'mailish_device_id',
                'AccountID': 'not_mailish_uid',
                'EventType': 'EVENT_INIT',
                'APIKey': '29733',
                'RegionTimeZone': '10800',
                'AppPlatform': 'iOs'
            },
            {
                'EventID': '2003',
                'DeviceID': 'signature_device_id',
                'AccountID': 'signature_uid',
                'EventType': 'EVENT_INIT',
                'APIKey': '29733',
                'RegionTimeZone': '10800',
                'AppPlatform': 'iOs'
            },
            {
                'EventID': '2004',
                'DeviceID': 'dark_theme_device_id',
                'AccountID': 'dark_theme_uid',
                'EventType': 'EVENT_INIT',
                'APIKey': '29733',
                'RegionTimeZone': '10800',
                'AppPlatform': 'iOs'
            },
            {
                'EventID': '2005',
                'DeviceID': 'mailish_device_id',
                'AccountID': 'mailish_uid',
                'EventName': 'some_event',
                'APIKey': '29733',
                'AppPlatform': 'iOs'
            },
            {
                'EventID': '2006',
                'DeviceID': 'not_logined_device_id',
                'AccountID': None,
                'EventType': 'EVENT_INIT',
                'APIKey': '29733',
                'RegionTimeZone': '10800',
                'AppPlatform': 'iOs'
            },
            {
                'EventID': '2007',
                'DeviceID': 'late_binding_device_id',
                'AccountID': None,
                'EventType': 'EVENT_INIT',
                'APIKey': '29733',
                'RegionTimeZone': '10800',
                'AppPlatform': 'iOs'
            },
            {
                'EventID': '2008',
                'DeviceID': 'late_binding_device_id',
                'AccountID': 'late_binding_uid',
                'EventName': 'some_event',
                'APIKey': '29733',
                'AppPlatform': 'iOs'
            }
        ],
        [
            {
                'EventID': '3000',
                'DeviceID': 'not_new_user_device_id',
                'AccountID': 'not_new_user_uid',
                'EventType': 'CUSTOM_EVENT',
                'APIKey': '29733'
            }
        ]
    ]
    prepare_metrika_tables(metrika_rows, client, yt_paths)
    prepare_xeno_tables([['mailish_uid']], client, yt_paths)
    prepare_tables(client, yt_paths)
    db = UserEventsDBYT(client, yt_paths)

    devices = db.get_fresh_installs()
    assert len(devices) == 6
    assert set([device['device_id'] for device in devices]) == \
        set(['new_user_device_id', 'mailish_device_id', 'signature_device_id',
            'dark_theme_device_id', 'late_binding_device_id', 'not_logined_device_id'])
    assert set([device['uid'] for device in devices if device['uid'] is not None]) == \
        set(['new_user_uid', 'not_mailish_uid', 'signature_uid', 'dark_theme_uid'])

    db.get_uids_for_new_devices(devices)
    assert set([device['uid'] for device in devices if device['uid'] is not None]) == \
        set(['new_user_uid', 'not_mailish_uid', 'signature_uid', 'dark_theme_uid',
            'late_binding_uid'])

    db.write_new_devices([device for device in devices if device['uid'] is not None])

    tasks = [
        {
            'data': {
                'step': 'signature',
                'device_id': 'new_user_device_id',
                'platform': 'ios'
            }
        },
        {
            'data': {
                'step': 'xeno',
                'device_id': 'mailish_device_id',
                'platform': 'ios'
            }
        },
        {
            'data': {
                'step': 'signature',
                'device_id': 'signature_device_id',
                'platform': 'ios'
            }
        },
        {
            'data': {
                'step': 'dark_theme',
                'device_id': 'dark_theme_device_id',
                'platform': 'ios'
            }
        },
    ]
    irrelevant_devices = db.check_relevance([task['data'] for task in tasks])
    assert irrelevant_devices == set(['signature_device_id', 'dark_theme_device_id'])

    user_device = db.get_new_uids_for_existing_devices()
    assert len(user_device) == 1
    assert user_device[0]['uid'] == 'mailish_uid'
    assert user_device[0]['device_id'] == 'mailish_device_id'

    db.write_uids_for_existing_devices(user_device)
    irrelevant_devices = db.check_relevance([task['data'] for task in tasks])
    assert irrelevant_devices == set(['mailish_device_id', 'signature_device_id', 'dark_theme_device_id'])
