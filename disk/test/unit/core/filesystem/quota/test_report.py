# -*- coding: utf-8 -*-
import mock

from hamcrest import assert_that, has_entries, instance_of, has_entry, equal_to

from test.unit.base import NoDBTestCase


class ReportTestCase(NoDBTestCase):
    """
    Проверяет метод Quota.report().

    Проверяет обработку передаваемых методу значений от сервисов disk_service, trash_service.
    """
    uid = 123

    test_cases = {
        "test_returns_all_fields": {
            "services": {
                "disk": {
                    "limit": 1000000000,
                    "used": 222222222,
                    "files_count": 10
                },
                "trash": {
                    "used": 111111111
                }
            }
        },
        "test_free_when_used_less_than_limit": {
            "services": {
                "disk": {
                    "limit": 10737418240,
                    "used": 5368709120,
                    "files_count": 1
                },
                "trash": {
                    "used": 0
                }
            }
        },
        "test_free_when_used_greater_than_limit": {
            "services": {
                "disk": {
                    "limit": 10737418240,
                    "used": 18253611008,
                    "files_count": 7
                },
                "trash": {
                    "used": 123
                }
            }
        }
    }

    def test_returns_all_fields(self):
        expected_entries = {
            'uid': self.uid,
            'limit': instance_of(int),
            'used': instance_of(int),
            'free': instance_of(int),
            'trash': instance_of(int),
            'files_count': instance_of(int),
            'filesize_limit': instance_of(int),
        }

        assert_that(self.report_result, has_entries(expected_entries),
                    "report result doesn't contain all necessary fields with correct types.")

    def test_free_when_used_less_than_limit(self):
        """
        Проверяет подсчет количества свободного места (поле 'free'), когда

            disk_service.used() <= disk_service.limit().

        """
        disk_params = self.case_params['services']['disk']
        expected_free = disk_params['limit'] - disk_params['used']

        assert_that(self.report_result, has_entry('free', equal_to(expected_free)))

    def test_free_when_used_greater_than_limit(self):
        """
        Проверяет подсчет количества свободного места (поле 'free'), когда

            disk_service.used() > disk_service.limit()

        """
        assert_that(self.report_result['free'], equal_to(0),
                    "'free' is not equal to 0, but it should be "
                    "(amount of used disk space greater than disk space limit)")

    def setup_method(self, method):
        test_name = method.__func__.func_name
        self.case_params = self.test_cases[test_name]
        self._patch_environment()
        self.report_result = self._get_report_result()

    def teardown_method(self, method):
        for patcher in self._patchers:
            patcher.stop()

    def _patch_environment(self):
        """
        Замещает окружение метода Quota.report mock-объектами, которые
        возвращают предопределенными значениями. Под окружением метода
        понимаются:
          * сервисы, у которых в методе запрашиваются данные;
        """
        from mpfs.core.services.disk_service import MPFSStorageService
        from mpfs.core.services.trash_service import Trash
        from mpfs.config import settings

        services = {
            'disk': MPFSStorageService,
            'trash': Trash,
        }

        self._patchers = []
        # Patch services
        for service_name, patch_methods in self.case_params['services'].items():
            for method, value in patch_methods.items():
                patcher = mock.patch.object(services[service_name], method,
                                            return_value=value)
                self._patchers.append(patcher)

        for patcher in self._patchers:
            patcher.start()

    def _get_report_result(self):
        """
        Возвращает результат метода Quota.report на конкретных значениях
        тестового кейса.
        """
        from mpfs.core.filesystem import quota

        quota = quota.Quota()
        return quota.report(self.uid)
