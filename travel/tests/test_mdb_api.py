import requests_mock

from travel.library.python.avia_mdb_replica_info.avia_mdb_replica_info import MdbAPI, const


def test_get_cluster_info_from_api():
    cluster_id = 'test'
    body = {
        "hosts": [
            {
                "assignPublicIp": False,
                "clusterId": "mdb9sssbmtcje8gtvlrc",
                "health": "ALIVE",
                "name": "sas-f4ldjv9z33rk81jy.db.yandex.net",
                "priority": "5",
                "resources": {
                    "diskSize": "322122547200",
                    "diskTypeId": "local-ssd",
                    "resourcePresetId": "db1.medium"
                },
                "role": "MASTER",
                "services": [
                    {
                        "health": "ALIVE",
                        "type": "POSTGRESQL"
                    },
                    {
                        "health": "ALIVE",
                        "type": "POOLER"
                    }
                ],
                "zoneId": "sas"
            },
            {
                "assignPublicIp": False,
                "clusterId": "mdb9sssbmtcje8gtvlrc",
                "health": "ALIVE",
                "name": "vla-i0102fi9nfr1puak.db.yandex.net",
                "priority": "0",
                "replicaType": "SYNC",
                "resources": {
                    "diskSize": "322122547200",
                    "diskTypeId": "local-ssd",
                    "resourcePresetId": "db1.medium"
                },
                "role": "REPLICA",
                "services": [
                    {
                        "health": "ALIVE",
                        "type": "POSTGRESQL"
                    },
                    {
                        "health": "ALIVE",
                        "type": "POOLER"
                    }
                ],
                "zoneId": "vla"
            }
        ]
    }

    with requests_mock.Mocker() as m:
        m.get('{}/clusters/{}/hosts'.format(const.POSTGRES_API_BASE_URL, cluster_id), json=body)
        m.post(const.IAM_TOKEN_DEAFULT_URL, json={})

        mdb_api = MdbAPI(api_base_url=const.POSTGRES_API_BASE_URL, oauth_token='test')
        cluster_info = mdb_api.get_cluster_info(cluster_id)

        assert cluster_info.instances[0].hostname == 'sas-f4ldjv9z33rk81jy.db.yandex.net'
        assert cluster_info.instances[0].is_master is True
        assert cluster_info.instances[1].hostname == 'vla-i0102fi9nfr1puak.db.yandex.net'
        assert cluster_info.instances[1].is_master is False


def test_default_cluster_info():
    mdb_api = MdbAPI(api_base_url=const.POSTGRES_API_BASE_URL, oauth_token='test')
    mdb_api.add_default_cluster_info('cluster42', 'sas.mymaster', ['sas.replica1', 'myt.replica3'])

    cluster_info = mdb_api.get_default_cluster_info('cluster42')
    assert cluster_info.cluster_id == 'cluster42'
    assert len(cluster_info.instances) == 3
    assert vars(cluster_info.instances[0]) == {
        'hostname': 'sas.replica1',
        'dc': 'sas',
        'is_master': False,
        'raw_data': None
    }
    assert vars(cluster_info.instances[1]) == {
        'hostname': 'myt.replica3',
        'dc': 'myt',
        'is_master': False,
        'raw_data': None
    }
    assert vars(cluster_info.instances[2]) == {
        'hostname': 'sas.mymaster',
        'dc': 'sas',
        'is_master': True,
        'raw_data': None
    }

    assert mdb_api.get_default_cluster_info('cluster666') is None
