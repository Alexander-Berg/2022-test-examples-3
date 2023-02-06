from __future__ import unicode_literals, absolute_import

from market.sre.tools.rtc import nanny


def test_create(monkeypatch,  manager):
    """
    :type monkeypatch: _pytest.monkeypatch.MonkeyPatch
    :type manager: market.sre.tools.rtc.nanny.manager.ServiceRepoManager
    """

    create_vals_def = {
        'self': None,
        'src_service': None,
        'service_id': None,
        'description': None,
        'service_category': None,
        'group': None,
        'gencfg_release': None,
        'owners_logins': None,
        'owners_groups': None,
        'managers_logins': None,
        'managers_groups': None,
        'push_client_secret_id': None,
        'market_front_secret_id': None,
        'abc_service_id': None,
        'allow_override': None,
        'activate': None,
        'slug': None,
    }
    create_vals = create_vals_def.copy()

    def create_mock(
        self,
        src_service,
        dst_service_id,
        description,
        service_category,
        group=None,
        gencfg_release=None,
        owners_logins=None,
        owners_groups=None,
        managers_logins=None,
        managers_groups=None,
        push_client_secret_id=None,
        market_front_secret_id=None,
        abc_service_id=None,
        allow_override=False,
        activate=False,
        slug=""
    ):
        create_vals['self'] = self
        create_vals['src_service'] = src_service
        create_vals['service_id'] = dst_service_id
        create_vals['description'] = description
        create_vals['service_category'] = service_category
        create_vals['group'] = group
        create_vals['gencfg_release'] = gencfg_release
        create_vals['owners_logins'] = owners_logins
        create_vals['owners_groups'] = owners_groups
        create_vals['managers_logins'] = managers_logins
        create_vals['managers_groups'] = managers_groups
        create_vals['push_client_secret_id'] = push_client_secret_id
        create_vals['market_front_secret_id'] = market_front_secret_id
        create_vals['abc_service_id'] = abc_service_id
        create_vals['allow_override'] = allow_override
        create_vals['activate'] = activate
        create_vals['slug'] = slug

    monkeypatch.setattr(nanny.CommonService, "smart_copy_service", create_mock)

    expected = {
        'service_id': 'service_id',
        'description': 'description',
        'service_category': 'service_category',
        'group': 'gencfg_group',
        'gencfg_release': 'gencfg_release',
        'owners_logins': ['owners_login'],
        'owners_groups': ['owners_group'],
        'managers_logins': ['owners_login'],
        'managers_groups': ['owners_group'],
        'abc_service_id': 1,
        'allow_override': True,
        'activate': True,
        'slug': ""
    }

    def mock_replace(*args, **kwargs):
        pass

    monkeypatch.setattr(nanny.CommonService, "replace_slug", mock_replace)

    common = nanny.CommonService(manager, "test")
    common._template_service = 1
    common.create(**expected)

    assert create_vals['self'] == common
    assert create_vals['src_service'] == 1
    for k, v in expected.items():
        assert create_vals[k] == v

    create_vals = create_vals_def.copy()

    monkeypatch.setattr(nanny.JavaService, "replace_application", mock_replace)

    java = nanny.JavaService(manager, "test", 0)
    java._template_service = 1
    java.create(**expected)

    assert create_vals['self'] == java
    assert create_vals['src_service'] == 1
    for k, v in expected.items():
        assert create_vals[k] == v

    create_vals = create_vals_def.copy()
    monkeypatch.setattr(nanny.NodejsService, "_replace_resource", mock_replace)

    node = nanny.NodejsService(manager, "test", 0, 0, "test")
    node._template_service = 1
    node.create(**expected)

    assert create_vals['self'] == node
    assert create_vals['src_service'] == 1
    for k, v in expected.items():
        assert create_vals[k] == v

    create_vals = create_vals_def.copy()
    monkeypatch.setattr(nanny.SingleBinaryService, "_replace_resource", mock_replace)

    single = nanny.SingleBinaryService(manager, "test", 0, 1, "test")
    single._template_service = 1
    single.create(**expected)

    assert create_vals['self'] == single
    assert create_vals['src_service'] == 1
    for k, v in expected.items():
        assert create_vals[k] == v
