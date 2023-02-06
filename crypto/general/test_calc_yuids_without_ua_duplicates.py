import pytest
import allure
from matching.device_matching.perfect.device_yuid_perfect_by_source import DevidYuids

class UserAgent():
    def __init__(self, type, version):
        self.version = version
        self.current_type = type
        self.types = {
            "d_w": "d|desk|windows|",
            "t_a": "m|tablet|google|android|",
            "t_ios": "m|tablet|apple|ios|",
            "m_a": "m|phone|lg|android|",
            "m_ios": "m|phone|apple|ios|"
        }

    def to_ua_profile(self):
        return self.types[self.current_type] + self.version


@pytest.mark.parametrize("uas , expected_count", [
    ({"968665201508182931": UserAgent("m_ios", "11.0.1")}, 1),

    ({"968665201508182921": UserAgent("m_ios", "11.9"),
      "968665201508182922": None,
      "968665201508182923": None}, 3),

    ({"968665201508182932": UserAgent("d_w", "1.0"),
      "968665201508182933": UserAgent("d_w", "1.0")}, 2),

    ({"968665201508182934": UserAgent("d_w", "1.0"),
      "968665201508182935": UserAgent("d_w", "2.0")}, 1),

    ({"968665201508182936": UserAgent("t_a", "1.0"),
      "968665201508182937": UserAgent("t_ios", "2.0")}, 2),

    ({"968665201508182938": UserAgent("d_w", "1.0"),
      "968665201508182939": UserAgent("d_w", "2.0")}, 1),

    ({"968665201508182911": UserAgent("m_ios", "11.9"),
      "968665201508182912": UserAgent("m_ios", "11.9"),
      "968665201508182913": UserAgent("m_ios", "12.0")}, 2),

    ({"968665201508182914": UserAgent("m_ios", "11.9"),
      "968665201508182915": UserAgent("m_ios", "11.9"),
      "968665201508182916": UserAgent("m_ios", "12.0"),
      "968665201508182917": UserAgent("m_ios", "13.0"),
      "968665201508182918": UserAgent("m_ios", "13.0"),
      "968665201508182919": UserAgent("m_ios", "13.1")}, 2),

    ({"968665201508182911": UserAgent("m_ios", "11.1"),
      "968665201508182912": UserAgent("m_ios", "11.1"),
      "968665201508182913": UserAgent("m_ios", "11.1"),
      "968665201508182914": UserAgent("m_ios", "11.1"),
      "968665201508182915": UserAgent("m_ios", "12.0"),
      "968665201508182916": UserAgent("m_ios", "13.0")}, 4),
])
def test_calc_yuids_without_ua_duplicates(uas, expected_count):
    result = DevidYuids().calc_yuids_without_ua_duplicates(uas.keys(), uas)
    allure.attach("input data", str(uas))
    allure.attach("result and expected", str(result) + "|" + str(expected_count))
    assert result == expected_count
