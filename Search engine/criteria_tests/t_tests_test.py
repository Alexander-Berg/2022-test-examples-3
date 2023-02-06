from pytest import approx

from search.metrics.ccc.ccc_lib.criteria.t_tests import PairedTTest, TTestInd


def test_paired_t_test():
    ptt = PairedTTest()
    result = ptt(
        [312, 242, 388, 340, 296, 254, 391, 402, 290],
        [300, 201, 232, 312, 220, 256, 328, 330, 231]
    )
    assert result.value == approx(3.649206)
    assert result.p_value == approx(0.006502, rel=1e-3)


def test_t_test_for_constant_sequences():
    # Both lead to division by zero if both sequences have zero variance
    for tt in [PairedTTest(), TTestInd()]:
        result = tt(
            [0, 0, 0],
            [0, 0, 0],
        )
        assert not result
        assert result.p_value is None
        assert result.messages == tt.CONSTANT_SEQUENCES_MESSAGES
