from django.test.runner import DiscoverRunner


from cars.carsharing.models.car_tags_history import CarTagsHistory


class CarsharingTestRunner(DiscoverRunner):
    """Skip telematics tests to be run with twisted trial"""

    def build_suite(self, *args, **kwargs):
        suite = super().build_suite(*args, **kwargs)
        suite = self._filter_suite(suite)
        return suite

    def _filter_suite(self, suite):
        suite_class = type(suite)
        filtered_suite = suite_class()

        for test in suite:
            if test.__module__.startswith('cars.telematics'):
                continue
            filtered_suite.addTest(test)

        return filtered_suite
