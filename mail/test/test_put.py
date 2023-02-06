def test_puts_config(xconf):
    xconf.put('service', 'foo', 'owner', b'123')
    configs = xconf.get('service', 'foo')
    assert len(configs) == 1
    c = configs[0]
    assert c.type == 'service'
    assert c.name == 'foo'
    assert c.owner_id == 'owner'
    assert bytes(c.settings) == b'123'

def test_rejects_stale_updates(xconf):
    revision = xconf.put('service', 'foo', 'owner', b'123')[0].revision
    assert revision > 0
    r = xconf.put('service', 'foo', 'owner', b'123', revision=revision-1)[0]
    assert r.revision == 0
    assert r.error == 'stale_revision'