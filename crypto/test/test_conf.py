import crypta.lib.python.bt.conf.conf as conf
import pytest


def test_simple_reference():
    config = """
    paths:
        root: root
        events: {{ this.paths.root }}/events
        requests: {{ this.paths.root }}/requests
    """
    rendered = conf.render_config(config)
    assert rendered.paths.root == 'root'
    assert rendered.paths.events == 'root/events'
    assert rendered.paths.requests == 'root/requests'


def test_transitive_reference():
    config = """
    paths:
        root: {{ env['ROOT'] }}
        sub1: {{ this.paths.root }}/sub1
        sub2: {{ this.paths.sub1 }}/sub2
        sub3: {{ this.paths.sub2 }}/sub3
        sub4: {{ this.paths.sub3 }}/sub4
    """
    env = {'ROOT': 'zero'}
    rendered = conf.render_config(config, env=env)
    assert rendered.paths.root == env['ROOT']
    assert rendered.paths.sub1 == env['ROOT'] + '/sub1'
    assert rendered.paths.sub2 == env['ROOT'] + '/sub1/sub2'
    assert rendered.paths.sub3 == env['ROOT'] + '/sub1/sub2/sub3'
    assert rendered.paths.sub4 == env['ROOT'] + '/sub1/sub2/sub3/sub4'


def test_combine_two_references():
    config = """
    left: LEFT
    right: RIGHT
    combined: {{ this.left }} and {{ this.right }}
    """
    rendered = conf.render_config(config)
    assert rendered.combined == rendered.left + ' and ' + rendered.right


def test_cyclic():
    config = """
    cyclic:
        a: {{ this.cyclic.b }}
        b: {{ this.cyclic.c }}
        c: {{ this.cyclic.a }}
    """
    with pytest.raises(conf.CyclicReference):
        rendered = conf.render_config(config)
        assert rendered


def test_unresolved():
    config = """
    reference: {{ this.unexisting.reference }}
    """
    with pytest.raises(conf.UnresolvedReference):
        rendered = conf.render_config(config)
        assert rendered


def test_list_of_references():
    config = """
    root: {{ env['root'] }}
    list:
        - {{ this.root }}/a
        - {{ this.root }}/b
        - {{ this.root }}/c
    """
    env = {'root': 'whaat'}
    rendered = conf.render_config(config, env=env)
    assert rendered.list[0] == env['root']+'/a'
    assert rendered.list[1] == env['root']+'/b'
    assert rendered.list[2] == env['root']+'/c'


def test_reference_dict():
    config = """
    some_referenced_dict:
        russia: moscow
        france: paris

    reference_to_dict: {{ this.some_referenced_dict }}
    """
    rendered = conf.render_config(config)
    assert rendered['reference_to_dict']['russia'] == 'moscow'
    assert rendered['reference_to_dict']['france'] == 'paris'
