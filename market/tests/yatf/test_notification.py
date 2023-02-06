# -*- coding: utf-8 -*-
import pytest
import tempfile
from hamcrest import assert_that, equal_to
from mock import patch, Mock

from market.idx.library.glue.proto.GlueConfig_pb2 import EReduceKeySchema
from market.idx.marketindexer.miconfig import MiConfig
from market.idx.yatf.resources.glue_config import GlueConfig
from market.idx.cron.cron_clt.lib.checked_glue_yt_tables.checked_glue_yt_tables import (
    read_glue_config,

    get_glue_table_owner,
    get_source_table_fields,

    get_abc_service_ticket,
    get_emails_by_owner,
    get_tvm_token,

    send_mail,
)
from market.idx.cron.cron_clt.lib.checked_glue_yt_tables.yatf.resources.abc_mock import (
    ABCServer
)

from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from market.idx.yatf.resources.yt_table_resource import YtTableResource

from yt.wrapper.ypath import ypath_join

import yatest


SHORT_OFFER_ID_SCHEMA = [
    {'required': True, 'name': 'business_id', 'type': 'uint32', 'sort_order': 'ascending'},
    {'required': True, 'name': 'offer_id', 'type': 'string', 'sort_order': 'ascending'},
]


@pytest.fixture(scope='module')
def external_table(yt_server):
    class YtExternalTable1(YtTableResource):
        def __init__(self, yt_stuff, path, link_paths, data):
            super(YtExternalTable1, self).__init__(
                yt_stuff=yt_stuff,
                path=path,
                data=data,
                link_paths=link_paths,
                attributes=dict(
                    dynamic=False,
                    external=False,
                    schema=SHORT_OFFER_ID_SCHEMA + [
                        {'required': False, 'name': 'column_a', 'type': 'int64'},
                    ]
                )
            )

    prefix = get_yt_prefix()
    path = ypath_join(prefix, 'external_table/2022-03-01')
    recent = ypath_join(prefix, 'external_table/recent')

    table = YtExternalTable1(
        yt_stuff=yt_server,
        path=path,
        link_paths=[recent],
        data=[
            {
                'business_id': 1,
                'offer_id': 'offer1',
                'column_a': 1,
            },
            {
                'business_id': 2,
                'offer_id': 'offer2',
                'column_a': 2,
            },
        ]
    )
    table.create()
    return table


@pytest.fixture(scope='module')
def glue_config(external_table):
    return GlueConfig(
        {
            'Fields': [
                {
                    'glue_id': 1,
                    'declared_cpp_type': 'INT64',
                    'target_name': 'a',
                    'is_from_datacamp': False,
                    'owner': 'marketindexer',
                    'source_name': 'ext1',
                    'source_table_path': external_table.link,
                    'source_field_path': 'column_a',
                    'destination_table_path': '//home/in/glue/ext1/recent',
                    'data_limit_per_table': 100,
                    'data_limit_per_offer': 50,
                    'reduce_key_schema': EReduceKeySchema.SHORT_OFFER_ID,
                },
            ],
        },
        'glue_config.json'
    )


@pytest.yield_fixture(scope='module')
def expected_token():
    yield 'some_vault_token'


@pytest.yield_fixture(scope='module')
def expected_ticket():
    yield 'some_abc_ticket'


def abc_callback(obj):
    return (
        '{'
        '"next":null,'
        '"previous":null,'
        '"results":['
        '   {"person":{"login":"kgorelov"}},'
        '   {"person":{"login":"kgorelov"}},'
        '   {"person":{"login":"hvost239"}},'
        '   {"person":{"login":"bzz13"}},'
        '   {"person":{"login":"krasnobaev"}},'
        '   {"person":{"login":"green-yeti"}},'
        '   {"person":{"login":"crossfider"}},'
        '   {"person":{"login":"razmser"}},'
        '   {"person":{"login":"bzz13"}},'
        '   {"person":{"login":"green-yeti"}}'
        ']'
        '}'
    )


@pytest.yield_fixture(scope='module')
def abc_server(expected_ticket):
    server = ABCServer(ticket=expected_ticket, callback=abc_callback)
    server.init()
    yield server


@pytest.yield_fixture(scope='module')
def miconfig_data(
        yt_server,
        glue_config,
        external_table,
        abc_server
):
    yield '''
[general]
working_dir = /
glue_config_path={glue_config_path}

[yt]
yt_proxy_primary = {yt_proxy}
yt_tokenpath =

[glue]
tvm_client_id = 2009733
tvm_secret_uuid = sec-01dq7m7g2pgs0rhhq3xhn1r4d8
mail_notification_enabled = true
abc_url = http://{abc_host}:{abc_port}

[checked_yt_tables]
enable=true
enable_prepare=true
'''.format(
        glue_config_path=glue_config.path,
        yt_proxy=yt_server.get_server(),
        abc_host=abc_server.host,
        abc_port=abc_server.port,
    )


@pytest.yield_fixture(scope='module')
def miconfig_path(miconfig_data):
    with tempfile.NamedTemporaryFile(mode='w+') as f:
        f.write(miconfig_data)
        f.flush()
        yield f.name


@pytest.yield_fixture(scope='module')
def mi_config(miconfig_path):
    ds_config_path = yatest.common.source_path(
        'market/idx/marketindexer/tests/datasources.conf'
    )
    yield MiConfig([miconfig_path], ds_config_path)


def test_miconfig_glue_tvm_secret_uuid(mi_config):
    assert_that(
        mi_config.glue_tvm_secret_uuid,
        equal_to('sec-01dq7m7g2pgs0rhhq3xhn1r4d8')
    )


def test_miconfig_glue_tvm_client_id(mi_config):
    assert_that(
        mi_config.glue_tvm_client_id,
        equal_to(2009733)
    )


def test_miconfig_glue_mail_notification_enabled(mi_config):
    assert_that(
        mi_config.glue_mail_notification_enabled,
        equal_to(True)
    )


def test_miconfig_abc_url(mi_config, abc_server):
    assert_that(
        mi_config.glue_abc_url,
        equal_to('http://{}:{}'.format(abc_server.host, abc_server.port))
    )


@pytest.yield_fixture(scope='module')
def patch_auth(expected_token, expected_ticket):
    with patch(
        'library.python.vault_client.client.'
        'VaultClient.get_version',
        return_value={'value': {'client_secret': expected_token}}
    ) as a, patch(
        'library.python.vault_client.client.'
        'VaultClient.get_status',
        return_value={'is_deprecated_client': False}
    ) as b, patch(
        'market.idx.cron.cron_clt.lib.'
        'checked_glue_yt_tables.checked_glue_yt_tables.'
        'TvmClient.__init__',
        return_value=None
    ) as c, patch(
        'market.idx.cron.cron_clt.lib.'
        'checked_glue_yt_tables.checked_glue_yt_tables.'
        'TvmClient.get_service_ticket_for',
        return_value=expected_ticket
    ) as d:
        yield a, b, c, d


def test_auth(
        patch_auth,
        mi_config,
        expected_token,
        expected_ticket
):
    token = get_tvm_token(mi_config)
    assert_that(
        token,
        equal_to(expected_token)
    )

    ticket = get_abc_service_ticket(mi_config, token)
    assert_that(
        ticket,
        equal_to(expected_ticket)
    )


def test_get_emails_by_owner(
        patch_auth,
        mi_config,
        yt_server,
        external_table
):
    yt_client = yt_server.get_yt_client()
    glue_config = read_glue_config(mi_config)
    fields = get_source_table_fields(glue_config)
    assert_that(
        len(fields),
        equal_to(1)
    )
    path = fields[0].source_table_path
    assert_that(
        path,
        equal_to(external_table.link)
    )

    owner = get_glue_table_owner(yt_client, path, glue_config)
    assert_that(
        owner,
        equal_to('marketindexer')
    )

    emails = get_emails_by_owner(mi_config, owner)
    actual_emails = list(sorted(emails))
    expected_emails = list(sorted([
        'bzz13@yandex-team.ru',
        'crossfider@yandex-team.ru',
        'green-yeti@yandex-team.ru',
        'hvost239@yandex-team.ru',
        'kgorelov@yandex-team.ru',
        'krasnobaev@yandex-team.ru',
        'razmser@yandex-team.ru',
    ]))
    assert_that(
        actual_emails,
        equal_to(expected_emails)
    )


def test_send_mail(monkeymodule):
    result = []

    def write_args(pmock, *args, **kwargs):
        result.extend(args[0][0])
        return pmock

    def write_message(*args, **kwargs):
        result.extend(args)

    popen_mock = Mock()
    attrs = {
        'side_effect': lambda *args, **kwargs: write_args(
            popen_mock, args, kwargs
        ),
        'stdin.write.side_effect': write_message,
        'stdin.close.return_value': 0,
    }
    popen_mock.configure_mock(**attrs)

    with monkeymodule.context() as m:
        m.setattr(
            'market.idx.cron.cron_clt.lib.'
            'checked_glue_yt_tables.checked_glue_yt_tables.'
            'subprocess.Popen',
            popen_mock
        )
        send_mail('subject', 'message', ['bzz13@yandex-team.ru'])
        assert_that(
            result,
            equal_to([
                'mail',
                '-s',
                'subject',
                'bzz13@yandex-team.ru',
                'message'
            ])
        )
