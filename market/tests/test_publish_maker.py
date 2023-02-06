import pytest

from market.idx.pylibrary.report_control.publish_maker import AsyncPublisherMaker, ReloadSettings


REPORT_GROUP = 'prod_report_parallel@parallel'


@pytest.fixture
def publisher(zk):
    return AsyncPublisherMaker(zk_client=zk)


@pytest.fixture
def reload_settings():
    return ReloadSettings(
        report_groups=[REPORT_GROUP],
        datacenters=['iva', 'man'],
        generation='20200909_0808',
        failures_threshold=0.3,
        min_alive=0.4,
        make_skynet_http_url=True,
        report_version='2020.03.1234',
        meta_report_version='2020.03.4321',
    )


@pytest.fixture
def publisher_config():
    return {
        'dcgroups': {
            "prod_report_parallel@parallel": {
                "async_publishing_mode": "enabled",
                "close_firewall_sleep": 15,
                "close_report_with_old_docs": 1800.0,
                "failures_threshold": 1,
                "generations_prefix": "generations",
                "hosts": {
                    "parallel.00.rtc.man0-0281.search.yandex.net@17050": {
                        "cluster": 0,
                        "datacenter": "man",
                        "dists": {
                            "book-part-0": {},
                            "model-part-0": {},
                            "search-cards": {},
                            "search-part-additions-0": {},
                            "search-part-additions-8": {},
                            "search-part-base-0": {},
                            "search-part-base-8": {},
                            "search-part-blue-0": {},
                            "search-report-data": {},
                            "search-stats": {},
                            "search-wizard": {}
                        },
                        "key": "rtc-ct:man0-0281.search.yandex.net:17050",
                        "name": "man0-0281-man-market-prod-repo-54a-17050.gencfg-c.yandex.net",
                        "port": 17053,
                        "redundancy": 1,
                        "rtc_host": "man0-0281.search.yandex.net",
                        "rtc_port": 17050,
                        "rtc_service": "prod_report_parallel_man",
                        "service": "marketsearch3"
                    }
                }
            }
        }
    }


def test_status(publisher):
    status = publisher.status(['report_api@atlantis'])

    assert status == {'report_api@atlantis': None}


def test_write_config(publisher, reload_settings, publisher_config):
    publisher.write_config(reload_settings, publisher_config)

    assert REPORT_GROUP in publisher.groups_with_cowboy_reload([REPORT_GROUP])


def test_write_config_in_dry_mode(publisher, reload_settings, publisher_config):
    publisher.write_config(reload_settings, publisher_config, dry_mode=True)

    assert REPORT_GROUP not in publisher.groups_with_cowboy_reload([REPORT_GROUP])


def test_reset_config(publisher, reload_settings, publisher_config):
    publisher.write_config(reload_settings, publisher_config)

    publisher.reset_config([REPORT_GROUP])

    assert REPORT_GROUP not in publisher.groups_with_cowboy_reload([REPORT_GROUP])


def test_reset_config_in_dry_mode(publisher, reload_settings, publisher_config):
    publisher.write_config(reload_settings, publisher_config)

    publisher.reset_config([REPORT_GROUP], dry_mode=True)

    assert REPORT_GROUP in publisher.groups_with_cowboy_reload([REPORT_GROUP])
