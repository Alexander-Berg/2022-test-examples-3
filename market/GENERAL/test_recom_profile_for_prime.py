#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa

from core.types import Offer
from core.testcase import TestCase, main

from market.proto.recom.exported_dj_user_profile_pb2 import (
    TEcomVersionedDjUserProfile,
    TVersionedProfileData,
    TBrandsDataV1,
)  # noqa pylint: disable=import-error


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.set_default_reqid = False

    @classmethod
    def prepare_recom_profile_fetching(cls):
        cls.index.offers += [
            Offer(title='брюки Nike'),
        ]

        profile = TEcomVersionedDjUserProfile(
            ProfileData=TVersionedProfileData(
                BrandsV2=TBrandsDataV1(Brands={4450903: 0.8, 4450905: 0.8}),
            )
        )

        cls.dj.on_request(yandexuid='242334', exp='fetch_user_profile_versioned').respond(
            profile_data=profile.SerializeToString(), is_binary_data=True
        )

    def test_profile_data_fetching(self):
        '''
        Проверяем, что под флагом fetch_recom_profile_for_prime происходит поход за рекомендательным профилем
        '''

        for client in ['', '&client=desktop']:
            request = 'place=prime&text=брюки&rearr-factors=fetch_recom_profile_for_prime=1&yandexuid=242334&debug=1{}'.format(
                client
            )
            response = self.report.request_json(request)
            self.assertFragmentIn(
                response,
                {"search": {"results": [{"entity": "offer", "titles": {"raw": 'брюки Nike'}}]}},
                preserve_order=True,
                allow_different_len=False,
            )

            self.assertFragmentIn(
                response,
                {"debug": {"report": {"logicTrace": [r"\[ME\].*? This is recom profile data"]}}},
                preserve_order=True,
                use_regex=True,
            )

        self.error_log.not_expect(code=3809)

    def test_sovetnik_no_profile(self):
        request = 'place=prime&text=брюки&rearr-factors=fetch_recom_profile_for_prime=1&yandexuid=242334&debug=1&client=sovetnik'
        response = self.report.request_json(request)
        self.assertFragmentNotIn(
            response,
            {
                "logicTrace": [
                    r"\[ME\].*? This is recom profile data",
                ]
            },
            preserve_order=True,
            use_regex=True,
        )

        self.error_log.not_expect(code=3809)

    def test_failed_fetching(self):
        '''
        Проверяем отправку в голован метрики ошибок
        '''
        request = 'place=prime&text=брюки&rearr-factors=fetch_recom_profile_for_prime=1&yandexuid=242336&debug=1'

        before = self.report.request_tass()
        response = self.report.request_json(request)
        after = self.report.request_tass_or_wait(wait_hole='fetch_recom_profile_on_prime_error_count_dmmm')

        self.assertEqual(
            before.get('fetch_recom_profile_on_prime_error_count_dmmm', 0) + 1,
            after.get('fetch_recom_profile_on_prime_error_count_dmmm', 0),
        )

        self.assertFragmentIn(
            response,
            {"search": {"results": [{"entity": "offer", "titles": {"raw": 'брюки Nike'}}]}},
            preserve_order=True,
            allow_different_len=False,
        )

        self.assertFragmentIn(
            response,
            {
                "debug": {
                    "report": {
                        "logicTrace": [
                            r"\[ME\].*? Recom profile data not found",
                        ]
                    }
                }
            },
            preserve_order=True,
            use_regex=True,
        )

        self.error_log.not_expect(code=3809)


if __name__ == '__main__':
    main()
