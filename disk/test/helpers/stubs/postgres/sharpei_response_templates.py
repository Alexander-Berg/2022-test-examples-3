SHARD_TEMPLATE = """
{
    "id": 1,
    "name": "fsdb01",
    "databases": [
        {
            "address": {
                "dataCenter": "PGAAS",
                "dbname": "%(dbname)s",
                "host": "%(host)s",
                "port": "%(port)s",
                "dataCenter": "IVA"
            },
            "role": "master",
            "state": {
                "lag": 0
            },
            "status": "alive"
        },
        {
            "address": {
                "dataCenter": "PGAAS",
                "dbname": "%(dbname)s",
                "host": "%(host)s",
                "port": "%(port)s",
                "dataCenter": "SAS"
            },
            "role": "replica",
            "state": {
                "lag": 0
            },
            "status": "alive"
        }
    ]
}
"""


CONNECTION_INFO_TEMPLATE = """
{
    "shard": %s
}
""" % SHARD_TEMPLATE


STAT_TEMPLATE = """
    {
    "1": {
        "databases": [
            {
                "address": {
                    "dataCenter": "PGAAS",
                    "dbname": "%(dbname)s",
                    "host": "%(host)s",
                    "port": "%(port)s"
                },
                "role": "master",
                "state": {
                    "lag": 0
                },
                "status": "alive"
            }
        ],
        "id": 1,
        "name": "fsdb01"
    }
}
"""
