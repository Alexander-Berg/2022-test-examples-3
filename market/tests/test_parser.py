import json
import pytest

from market.idx.devtools.common_proxy_monitor.lib.parser import StatusParser


@pytest.fixture
def service_status():
    return json.loads('''
        {
            "config": {
                "Proxy": [
                    {
                        "Links": [
                            {
                                "Link": [
                                    {
                                        "Enabled": "1",
                                        "From": "LbkInput",
                                        "RpsLimit": "0",
                                        "To": "ProtoUnpacker"
                                    },
                                    {
                                        "Enabled": "1",
                                        "From": "ProtoUnpacker",
                                        "RpsLimit": "0",
                                        "To": "YtUCInfoSender"
                                    },
                                    {
                                        "Enabled": "1",
                                        "From": "ProtoUnpacker",
                                        "RpsLimit": "0",
                                        "To": "YtUCInfoSenderReplica"
                                    }
                                ]
                            }
                        ],
                        "Processors": [
                            {
                                "LbkInput": [
                                    {
                                        "InflightCount": "100",
                                        "MaxCount": "100",
                                        "MaxInProcess": "100",
                                        "MaxMemoryUsage": "104857600",
                                        "MaxQueue": "10000",
                                        "MaxSize": "10485760",
                                        "MaxUncommittedCount": "0",
                                        "MaxUncommittedSize": "0",
                                        "Threads": "10",
                                        "TimeoutSec": "10",
                                        "Type": "LOGBROKER_READER"
                                    }
                                ],
                                "ProtoUnpacker": [
                                    {
                                        "MaxQueue": "5000",
                                        "Threads": "10",
                                        "Type": "UC_PROTO_UNPACKER"
                                    }
                                ],
                                "YtUCInfoSender": [
                                    {
                                        "MaxQueue": "1000",
                                        "Threads": "20",
                                        "Type": "YT_UC_INFO_TABLE_WRITER"
                                    }
                                ],
                                "YtUCInfoSenderReplica": [
                                    {
                                        "MaxQueue": "1000",
                                        "Threads": "20",
                                        "Type": "YT_UC_INFO_TABLE_WRITER"
                                    }
                                ]
                            }
                        ],
                        "StartTimeout": "20.000000s"
                    }
                ]
            },
            "links": {
                "LbkInput->ProtoUnpacker": {
                    "dropped_count": 0,
                    "dropped_per_second": {
                        "1": 0,
                        "10": 0,
                        "30": 0,
                        "60": 0,
                        "180": 0,
                        "300": 0,
                        "600": 0
                    },
                    "forwarded_count": 2866194,
                    "forwarded_per_second": {
                        "1": 73,
                        "10": 58.20000076,
                        "30": 49.59999847,
                        "60": 53.68333435,
                        "180": 49.10555649,
                        "300": 47.6566658,
                        "600": 56.64500046
                    }
                },
                "ProtoUnpacker->YtUCInfoSender": {
                    "dropped_count": 0,
                    "dropped_per_second": {
                        "1": 0,
                        "10": 0,
                        "30": 0,
                        "60": 0,
                        "180": 0,
                        "300": 0,
                        "600": 0
                    },
                    "forwarded_count": 2866194,
                    "forwarded_per_second": {
                        "1": 73,
                        "10": 58.20000076,
                        "30": 49.59999847,
                        "60": 53.68333435,
                        "180": 49.10555649,
                        "300": 47.6566658,
                        "600": 56.64500046
                    }
                },
                "ProtoUnpacker->YtUCInfoSenderReplica": {
                    "dropped_count": 0,
                    "dropped_per_second": {
                        "1": 0,
                        "10": 0,
                        "30": 0,
                        "60": 0,
                        "180": 0,
                        "300": 0,
                        "600": 0
                    },
                    "forwarded_count": 2866194,
                    "forwarded_per_second": {
                        "1": 73,
                        "10": 58.20000076,
                        "30": 49.59999847,
                        "60": 53.68333435,
                        "180": 49.10555649,
                        "300": 47.6566658,
                        "600": 56.64500046
                    }
                }
            },
            "load_average": 7.84,
            "mem_size_real": "921.246",
            "mem_size_virtual": "2027.316",
            "processors": {
                "LbkInput": {
                    "failed_count": 0,
                    "failed_per_second": {
                        "1": 0,
                        "10": 0,
                        "30": 0,
                        "60": 0,
                        "180": 0,
                        "300": 0,
                        "600": 0
                    },
                    "in_process": 12,
                    "processed_count": 2866194,
                    "processed_per_second": {
                        "1": 73,
                        "10": 58.20000076,
                        "30": 49.59999847,
                        "60": 53.68333435,
                        "180": 49.10555649,
                        "300": 47.6566658,
                        "600": 56.64500046
                    },
                    "queue_size": 1,
                    "time_per_request": 3.488947364e-7,
                    "work_time": 1
                },
                "ProtoUnpacker": {
                    "processed_count": 2866194,
                    "processed_per_second": {
                        "1": 73,
                        "10": 58.20000076,
                        "30": 49.59999847,
                        "60": 53.68333435,
                        "180": 49.10555649,
                        "300": 47.6566658,
                        "600": 56.64500046
                    },
                    "queue_size": 2,
                    "time_per_request": 0.1849438663,
                    "unpack_error_counter_count": 0,
                    "unpack_error_counter_per_second": {
                        "1": 0,
                        "10": 0,
                        "30": 0,
                        "60": 0,
                        "180": 0,
                        "300": 0,
                        "600": 0
                    },
                    "unpack_success_counter_count": 87462547,
                    "unpack_success_counter_per_second": {
                        "1": 2337,
                        "10": 1718.400024,
                        "30": 1604.43335,
                        "60": 1903.483276,
                        "180": 1719.677734,
                        "300": 1574.696655,
                        "600": 1966.93335
                    },
                    "work_time": 530085
                },
                "YtUCInfoSender": {
                    "collision_counter_count": 20,
                    "collision_counter_per_second": {
                        "1": 0,
                        "10": 0,
                        "30": 0,
                        "60": 0,
                        "180": 0,
                        "300": 0,
                        "600": 0
                    },
                    "error_counter_count": 0,
                    "error_counter_per_second": {
                        "1": 0,
                        "10": 0,
                        "30": 0,
                        "60": 0,
                        "180": 0,
                        "300": 0,
                        "600": 0
                    },
                    "insert_counter_count": 2866182,
                    "insert_counter_per_second": {
                        "1": 71,
                        "10": 57.40000153,
                        "30": 49.23333359,
                        "60": 53.51666641,
                        "180": 49.04444504,
                        "300": 47.61999893,
                        "600": 56.625
                    },
                    "processed_count": 2866182,
                    "processed_per_second": {
                        "1": 71,
                        "10": 57.40000153,
                        "30": 49.23333359,
                        "60": 53.51666641,
                        "180": 49.04444504,
                        "300": 47.61999893,
                        "600": 56.625
                    },
                    "queue_size": 3,
                    "skip_counter_count": 19433,
                    "skip_counter_per_second": {
                        "1": 0,
                        "10": 0,
                        "30": 0,
                        "60": 0,
                        "180": 0,
                        "300": 0,
                        "600": 0
                    },
                    "time_per_request": 106.3497719,
                    "update_counter_count": 2866182,
                    "update_counter_per_second": {
                        "1": 71,
                        "10": 57.40000153,
                        "30": 49.23333359,
                        "60": 53.51666641,
                        "180": 49.04444504,
                        "300": 47.61999893,
                        "600": 56.625
                    },
                    "work_time": 304817802
                },
                "YtUCInfoSenderReplica": {
                    "collision_counter_count": 11,
                    "collision_counter_per_second": {
                        "1": 0,
                        "10": 0,
                        "30": 0,
                        "60": 0,
                        "180": 0,
                        "300": 0,
                        "600": 0
                    },
                    "error_counter_count": 0,
                    "error_counter_per_second": {
                        "1": 0,
                        "10": 0,
                        "30": 0,
                        "60": 0,
                        "180": 0,
                        "300": 0,
                        "600": 0
                    },
                    "insert_counter_count": 2866194,
                    "insert_counter_per_second": {
                        "1": 74,
                        "10": 58.20000076,
                        "30": 49.66666794,
                        "60": 53.70000076,
                        "180": 49.10555649,
                        "300": 47.65999985,
                        "600": 56.64500046
                    },
                    "processed_count": 2866194,
                    "processed_per_second": {
                        "1": 74,
                        "10": 58.20000076,
                        "30": 49.66666794,
                        "60": 53.70000076,
                        "180": 49.10555649,
                        "300": 47.65999985,
                        "600": 56.64500046
                    },
                    "queue_size": 4,
                    "skip_counter_count": 19434,
                    "skip_counter_per_second": {
                        "1": 0,
                        "10": 0,
                        "30": 0,
                        "60": 0,
                        "180": 0,
                        "300": 0,
                        "600": 0
                    },
                    "time_per_request": 49.98272134,
                    "update_counter_count": 2866194,
                    "update_counter_per_second": {
                        "1": 74,
                        "10": 58.20000076,
                        "30": 49.66666794,
                        "60": 53.70000076,
                        "180": 49.10555649,
                        "300": 47.65999985,
                        "600": 56.64500046
                    },
                    "work_time": 143260176
                }
            }
        }
        ''')


@pytest.yield_fixture
def service_status_malformed():
    return json.loads('''
        {
            "processors": {
                "A": {
                    "queue_size": 10
                },
                "B": {
                    "queue_size": 0
                }
            },
            "links": {
                "A->B": {
                }
            }
        }
        ''')


@pytest.yield_fixture
def service_status_incomplete():
    return json.loads('''
        {
            "config": {
                "Proxy": [
                    {
                        "Processors": [
                            {
                                "A": [
                                    {
                                        "InflightCount": "100",
                                        "MaxCount": "100",
                                        "MaxInProcess": "100",
                                        "MaxMemoryUsage": "104857600",
                                        "MaxQueue": "10000",
                                        "MaxSize": "10485760",
                                        "MaxUncommittedCount": "0",
                                        "MaxUncommittedSize": "0",
                                        "Threads": "10",
                                        "TimeoutSec": "10",
                                        "Type": "A"
                                    }
                                ]
                            }
                        ]
                    }
                ]
            },
            "processors": {
                "A": {
                    "queue_size": 10
                },
                "B": {
                    "queue_size": 0
                }
            },
            "links": {
                "A->B": {
                }
            }
        }
        ''')


def test_parser(service_status):
    # arrange
    parser = StatusParser()

    # act
    ok = parser.parse(service_status)

    # assert
    assert ok
    assert list(sorted(parser.processors)) == ['LbkInput', 'ProtoUnpacker', 'YtUCInfoSender', 'YtUCInfoSenderReplica']
    assert parser.incoming_links == {'ProtoUnpacker': ['LbkInput'], 'YtUCInfoSender': ['ProtoUnpacker'], 'YtUCInfoSenderReplica': ['ProtoUnpacker']}
    assert parser.outgoing_links == {'LbkInput': ['ProtoUnpacker'], 'ProtoUnpacker': ['YtUCInfoSender', 'YtUCInfoSenderReplica']}
    assert parser.queue_sizes == {'LbkInput': 1, 'ProtoUnpacker': 2, 'YtUCInfoSender': 3, 'YtUCInfoSenderReplica': 4}
    assert parser.max_queue_sizes == {'LbkInput': 10000, 'ProtoUnpacker': 5000, 'YtUCInfoSender': 1000, 'YtUCInfoSenderReplica': 1000}


def test_malformed(service_status_malformed):
    # arrange
    parser = StatusParser()

    # act
    ok = parser.parse(service_status_malformed)

    # assert
    assert not ok


def test_incomplete(service_status_incomplete):
    # arrange
    parser = StatusParser()

    # act
    ok = parser.parse(service_status_incomplete)

    # assert
    assert ok
    assert parser.failed_processors == {'B'}


def test_rank(service_status):
    # arrange
    parser = StatusParser()

    # act
    assert parser.parse(service_status)

    # assert
    assert parser.rank('LbkInput') == 0
    assert parser.rank('ProtoUnpacker') == 1
    assert parser.rank('YtUCInfoSender') == 2
    assert parser.rank('YtUCInfoSenderReplica') == 2
