import pytest
from market.sre.tools.market_alerts_configurator.lib.juggler_config import JugglerConfig


@pytest.mark.parametrize('juggler_sections', [{'default': {'host': 'testhost'}, 'checks': []}])
def test__normalize_sections_types(juggler_sections, args):
    juggler_config = JugglerConfig(args, juggler_sections, create_api=False)
    return juggler_config._sections


@pytest.mark.parametrize('juggler_sections', [
    {
        'default': {
            'host': 'testhost',
            'tags': ['a_mark_testhost', "test_tag"],
            'namespace': 'market.sre',
            'description': 'description'
        },
        'checks': [
            {
                'service': 'ping',
                'check_tags': {'market_billing_phone'},
                'flaps': {
                    'stable_time': 900,
                    'critical_time': 3600,
                    'boost_time': 0
                },
                'notifications': [
                    {
                        'template_name': 'startrek',
                        'template_kwargs': {
                            'queue': 'CSADMIN',
                            'status': ['CRIT']
                        },
                        'description': 'description'
                    }
                ],
                'children': ['my_children']
            },
            {'service': 'with_default_flaps', 'flaps': 'default'},
            {'service': 'without_flaps'}
        ]
    }
])
def test__make_check_like_ansible_dict(juggler_sections, args):
    juggler_config = JugglerConfig(args, juggler_sections, create_api=False)
    juggler_config.make_checks()
    ansible_checks = []
    for check in juggler_config.checks:
        juggler_config._prepare_check(check)
        ansible_check = juggler_config._make_check_like_ansible_dict(check)
        ansible_checks.append(ansible_check)
    return ansible_checks
