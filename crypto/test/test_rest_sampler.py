from crypta.lib.python.sampler import rest_sampler


def test_passes_equal():
    assert rest_sampler.PassesEqual(value=1, denominator=100, rest=1)
    assert not rest_sampler.PassesEqual(value=0, denominator=100, rest=1)


def test_passes_less():
    assert rest_sampler.PassesLess(value=9, denominator=100, rest=10)
    assert not rest_sampler.PassesLess(value=10, denominator=100, rest=10)
    assert not rest_sampler.PassesLess(value=11, denominator=100, rest=10)
