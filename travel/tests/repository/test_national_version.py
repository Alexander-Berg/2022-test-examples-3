# coding=utf-8
from __future__ import absolute_import

from django.conf import settings
from mock import Mock

from travel.avia.library.python.tester.testcase import TestCase

from travel.avia.backend.repository.national_version import NationalVersionRepository


class TestNationalVersionRepository(TestCase):
    def setUp(self):
        self._repository = NationalVersionRepository(
            logger=Mock()
        )

    def test_all_national_version(self):
        self._repository.pre_cache()
        all_national_version = settings.AVIA_NATIONAL_VERSIONS
        code_to_id = {}

        for nv in all_national_version:
            model_id = self._repository.code_to_id(nv)
            code = self._repository.id_to_code(model_id)

            assert isinstance(model_id, long), nv
            assert isinstance(code, basestring), nv

            code_to_id[code] = model_id

        assert sorted(code_to_id.keys()) == sorted(all_national_version)
        assert len(set(code_to_id.values())) == len(all_national_version)
