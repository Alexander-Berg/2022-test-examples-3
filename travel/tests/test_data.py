import pytest
import logging
import sys

from travel.hotels.tools.permaroom_builder.builder.builder import FeatureReader, BedsReader


@pytest.fixture(scope="session", autouse=True)
def prepare_log():
    logging.basicConfig(level=logging.INFO, format="%(asctime)-15s | %(module)s | %(levelname)s | %(message)s",
                        stream=sys.stdout)


def test_features():
    reader = FeatureReader(skip_errors=True)
    reader.read_features('expedia_mapping.csv')
    reader.read_features('travelline_mapping.csv')
    assert not reader.has_features_errors, 'Some errors occured while reading features'


def test_beds():
    reader = BedsReader(skip_errors=True)
    reader.read_beds('travelline_beds_mapping.csv')
    reader.read_beds('expedia_beds_mapping.csv')
    assert not reader.has_errors, 'Some errors occured while reading beds'
