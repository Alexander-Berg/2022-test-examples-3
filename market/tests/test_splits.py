import pytest

from market.front.tools.server_exps.lib import UnknownHost, HostConfig, \
    make_services_queue, explicit_hosts, generate_exp_config


def test_make_services_queue__simple():
    result = make_services_queue({
        'a': HostConfig(4, 0),
        'b': HostConfig(4, 0),
    })

    assert result == ['a', 'b', 'a', 'b', 'a', 'b', 'a', 'b']


def test_make_services_queue__complex():
    result = make_services_queue({
        'a': HostConfig(8, 0),
        'b': HostConfig(4, 0),
        'c': HostConfig(2, 0),
    })

    assert result == ['a', 'b', 'c', 'a', 'a', 'b', 'a', 'a', 'b', 'c', 'a', 'a', 'b', 'a']


def test_make_services_queue__used():
    result = make_services_queue({
        'a': HostConfig(8, 0),
        'b': HostConfig(8, 4),
        'c': HostConfig(8, 6),
    })

    assert result == ['a', 'a', 'a', 'a', 'a', 'b', 'a', 'b', 'a', 'c', 'b', 'a', 'c', 'b']


def test_explicit_hosts():
    hosts_conf = {
        'a': HostConfig(4, 0),
        'b': HostConfig(4, 0),
    }

    services = {
        'a': ['a1', 'a2', 'a3', 'a4'],
        'b': ['b1', 'b2', 'b3', 'b4'],
    }

    result = explicit_hosts(['a1', 'a4', 'b3'], services, hosts_conf)

    assert result == ['a1', 'a4', 'b3']

    assert hosts_conf['a'].used == 2
    assert hosts_conf['b'].used == 1

    assert services == {
        'a': ['a2', 'a3'],
        'b': ['b1', 'b2', 'b4'],
    }


def test_explicit_hosts__fail():
    with pytest.raises(UnknownHost):
        explicit_hosts(['a1', 'a4', 'b3'], {}, {})


def test_generate_exp_config__const():
    result = generate_exp_config({'q': 4, 'w': 4}, {
        'a': ['a1', 'a2', 'a3', 'a4'],
        'b': ['b1', 'b2', 'b3', 'b4'],
    })

    assert result == {
        'q': ['a1', 'b1', 'a2', 'b2'],
        'w': ['a3', 'b3', 'a4', 'b4'],
    }


def test_generate_exp_config__percent():
    result = generate_exp_config({'q': 0.5, 'w': 0.5}, {
        'a': ['a1', 'a2', 'a3', 'a4'],
        'b': ['b1', 'b2', 'b3', 'b4'],
    })

    assert result == {
        'q': ['a1', 'b1', 'a2', 'b2'],
        'w': ['a3', 'b3', 'a4', 'b4'],
    }


def test_generate_exp_config__explicit():
    result = generate_exp_config({
        'q': ['a1', 'b1', 'a2', 'b2'],
        'w': ['a3', 'b3', 'a4', 'b4']
    }, {
        'a': ['a1', 'a2', 'a3', 'a4'],
        'b': ['b1', 'b2', 'b3', 'b4'],
    })

    assert result == {
        'q': ['a1', 'b1', 'a2', 'b2'],
        'w': ['a3', 'b3', 'a4', 'b4'],
    }


def test_generate_exp_config__mixed():
    result = generate_exp_config({
        'q': 0.5,
        'w': 2,
        'e': ['a2', 'b3']
    }, {
        'a': ['a1', 'a2', 'a3', 'a4'],
        'b': ['b1', 'b2', 'b3', 'b4'],
    })

    assert result == {
        'q': ['a1', 'b1', 'a3', 'b2'],
        'w': ['a4', 'b4'],
        'e': ['a2', 'b3'],
    }
