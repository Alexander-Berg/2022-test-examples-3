from unittest.case import TestCase
from yaml import SafeLoader, load as load_yaml

from library.python import resource

from travel.hotels.tools.region_pages_builder.common.tanker_data import TankerDataStorage


class TestTankerData(TestCase):
    def load_test_storage(self):
        raw_tanker_dict = load_yaml(resource.find('tanker-dict.yaml').decode('utf-8'), Loader=SafeLoader)
        return TankerDataStorage(raw_tanker_dict)

    def test_tanker_data_storage_loads_data(self):
        storage = self.load_test_storage()
        assert len(storage.get_region_page(1, "some", "city", None, None).content) == 8  # default template

    def test_tanker_data_storage_fetch_region_type_template(self):
        storage = self.load_test_storage()
        assert len(storage.get_region_page(1, "some", "other", None, None).content) == 1  # template for other region
