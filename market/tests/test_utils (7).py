# coding: utf-8


from market.sre.tools.dpreparer.lib.utils import get_container_spec


def test_get_container_spec(spec_container_good, spec_container_bad):
    spec_good = get_container_spec(spec_container_good)
    spec_bad = get_container_spec(spec_container_bad)

    assert list(spec_good.keys()) == [
        'spec',
        'resource_cache_spec',
        'resource_requests',
        'ip6_address_allocations',
        'ip6_subnet_allocations',
        'dns',
        'secrets',
        'pod_dynamic_attributes',
        'spec_timestamp',
        'node_entry',
        'immutable_meta'
    ]
    assert spec_bad == {}
