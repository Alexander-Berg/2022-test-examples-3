from search.martylib.test_utils import TestCase
from search.mon.workplace.src.libs.catalog.catalog_exceptions import ValidationError
from search.mon.workplace.src.libs.catalog.validators import weight_is_valid


class TestWorkplaceUtils(TestCase):
    def test_weight_is_valid(self):
        with self.assertRaises(ValidationError):
            weight_is_valid(1.1)
        with self.assertRaises(ValidationError):
            weight_is_valid(-1)
        with self.assertRaises(ValidationError):
            weight_is_valid(None)
        assert weight_is_valid(1) is None
