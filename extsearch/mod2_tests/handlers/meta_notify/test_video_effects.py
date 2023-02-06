

def test_get_effect(effect_getter, effect_mock_meta, effect_mock_channel):
    effects, inherited_effects = effect_getter.get_effects(effect_mock_meta, effect_mock_channel)
    assert len(inherited_effects) == 3
    assert len(effects) == 1
