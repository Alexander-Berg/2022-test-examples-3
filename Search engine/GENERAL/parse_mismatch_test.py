import pytest

from mismatch import build_observation_log_url, parse_log_message

log_string = (
    "YT Compare: observationResultsFromFallback != observationResultsFromDt for Observation{leftId=33838771, "
    "rightId=33838752, regionalType=WORLD, evaluationType=WEB}, first mismatch at 0: "
    "ObservationResultImpl{computationKey={onlySearchResult, onlySearchResult, true, true, "
    "regionality-hyp-cg-5, regionality-hyp-cg-5, false}, left=0.145647 (1298), right=0.151374 (1298), "
    "diff=-0.005727 (1), pValue=0.9941340212596752, wins=41, losses=66, queryResults=[], "
    "observation=Observation{leftId=33838771, rightId=33838752, regionalType=WORLD, evaluationType=WEB}, "
    "leftDate=Thu Jul 08 19:34:18 MSK 2021, rightDate=Thu Jul 08 19:34:17 MSK 2021, leftOverFresh={}, "
    "rightOverFresh={}, leftMetricDate=Thu Jul 08 19:34:18 MSK 2021, rightMetricDate=Thu Jul 08 19:34:17 "
    "MSK 2021, checkResults=[], signification=LIGHT_RED, "
    "leftSerpVersion='ceffc060-2854-4b3b-8cf8-73910dfa4e18', "
    "rightSerpVersion='a5c06aa5-1702-4ab6-882b-4d503623a918', notCalculated=false} != "
    "ObservationResultImpl{computationKey={onlySearchResult, onlySearchResult, true, true, "
    "regionality-hyp-cg-5, regionality-hyp-cg-5, false}, left=0.145647 (1298), right=0.151374 (1298), "
    "diff=-0.005727 (1), pValue=0.9941340212596752, wins=41, losses=66, queryResults=[], "
    "observation=Observation{leftId=33838771, rightId=33838752, regionalType=WORLD, evaluationType=WEB}, "
    "leftDate=Thu Jul 08 19:34:18 MSK 2021, rightDate=Thu Jul 08 19:34:17 MSK 2021, leftOverFresh={}, "
    "rightOverFresh={}, leftMetricDate=Thu Jul 08 19:34:18 MSK 2021, rightMetricDate=Thu Jul 08 19:34:17 "
    "MSK 2021, checkResults=[], signification=LIGHT_RED, "
    "leftSerpVersion='ceffc060-2854-4b3b-8cf8-73910dfa4e18', "
    "rightSerpVersion='a5c06aa5-1702-4ab6-882b-4d503623a918', notCalculated=false}, "
    "observationResultsFromFallback.size = 14, observationResultsFromDt.size = 14"
)


@pytest.fixture
def parsed_message():
    return parse_log_message(log_string)


def test_parse(parsed_message):
    assert parsed_message
    assert parsed_message["observation"] == "Observation{leftId=33838771, rightId=33838752, regionalType=WORLD, evaluationType=WEB}"
    assert parsed_message["first_left_ssv"] == "ceffc060-2854-4b3b-8cf8-73910dfa4e18"
    assert parsed_message["first_right_ssv"] == "a5c06aa5-1702-4ab6-882b-4d503623a918"
    assert parsed_message["second_left_ssv"] == "ceffc060-2854-4b3b-8cf8-73910dfa4e18"
    assert parsed_message["second_right_ssv"] == "a5c06aa5-1702-4ab6-882b-4d503623a918"


def test_log_url(parsed_message):
    assert build_observation_log_url(parsed_message) == "https://deploy.yandex-team.ru/stages/" \
                                                        "metrics_calculation_production/logs?deploy-unit=" \
                                                        "backend&query=message%3D%22Observation%7B" \
                                                        "leftId%3D33838771%2C+" \
                                                        "rightId%3D33838752%2C+" \
                                                        "regionalType%3DWORLD%2C+" \
                                                        "evaluationType%3DWEB%7D%22"
