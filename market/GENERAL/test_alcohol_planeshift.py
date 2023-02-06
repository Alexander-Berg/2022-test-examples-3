#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    DeliveryBucket,
    HyperCategory,
    Model,
    Offer,
    Outlet,
    OutletLegalInfo,
    OutletLicense,
    PickupBucket,
    PickupOption,
    Shop,
)
from core.testcase import TestCase, main
from core.types.hypercategory import ALCOHOL_VINE_CATEG_ID


class _Outlets(object):
    def __outlet(id, status):
        return Outlet(
            point_id=id,
            fesh=549083,
            region=213,
            point_type=Outlet.FOR_PICKUP,
            working_days=[i for i in range(10)],
            legal_info=OutletLegalInfo(),
            licenses=[OutletLicense(number=str(id), issue_date='2019-01-01', expiry_date='2021-01-01', status=status)],
        )

    outlet_status_success = __outlet(id=67137, status='SUCCESS')
    outlet_status_new = __outlet(id=987987123, status='NEW')


class T(TestCase):
    @classmethod
    def prepare(cls):
        # This setting is set in report config for planeshift environment
        cls.settings.allow_status_new_for_alco_licence = True
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']

        cls.index.shops += [
            Shop(fesh=549083, priority_region=213, phone="+7222998989", phone_display_options='*'),
        ]

        cls.index.hypertree += [HyperCategory(hid=ALCOHOL_VINE_CATEG_ID)]
        cls.index.outlets += [_Outlets.outlet_status_new, _Outlets.outlet_status_success]

        cls.index.models += [
            Model(hyperid=1, hid=ALCOHOL_VINE_CATEG_ID, title="Wine 1"),
            Model(hyperid=2, hid=ALCOHOL_VINE_CATEG_ID, title="Wine 2"),
        ]

        cls.index.offers += [
            Offer(
                fesh=549083,
                hid=ALCOHOL_VINE_CATEG_ID,
                alcohol=True,
                hyperid=1,
                title="Carmenere",
                offerid="Cmnr",
                pickup_buckets=[1],
            ),
            Offer(
                fesh=549083,
                hid=ALCOHOL_VINE_CATEG_ID,
                alcohol=True,
                hyperid=2,
                title="Shiraz",
                offerid="Shrz",
                pickup_buckets=[2],
            ),
        ]

        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=bid,
                fesh=549083,
                carriers=[99],
                options=[PickupOption(outlet_id=outlet.point_id, day_from=1, day_to=2, price=100)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            )
            for bid, outlet in ((1, _Outlets.outlet_status_success), (2, _Outlets.outlet_status_new))
        ]

    def test_alco_license_on_planeshift(self):
        """Плейншифт - единственный тип окружения, на котором допускается тип лицензии NEW"""
        for hyperid, outlet in ((1, _Outlets.outlet_status_success), (2, _Outlets.outlet_status_new)):
            offerinfo_request = "place=prime&hyperid={}&regset=2&rids=213&show-urls=direct&adult=1".format(hyperid)
            self.assertFragmentIn(
                self.report.request_json(offerinfo_request),
                {
                    "entity": "outlet",
                    "id": str(outlet.point_id),
                    "legalInfo": {"licence": {"number": str(outlet.licenses[0].number)}},
                },
            )


if __name__ == '__main__':
    main()
