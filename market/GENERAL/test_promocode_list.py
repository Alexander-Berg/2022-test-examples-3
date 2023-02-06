import market.mars.lite.env as env
import copy

from datetime import datetime

from market.mars.lib.promo.promocode.proto.promos_pb2 import TPrice, TImage, TPromo, TSorts, EPromoSource
from market.mars.lib.promo.promocode.proto.sorting_pb2 import EPromocodeSortType
from market.pylibrary.lite.matcher import Regex
from market.mars.lite.core.dyno import TPromoQuery


PROMO_IMAGES = [
    "//avatars.mds.yandex.net/get-mpic/4322107/img_id8962600163296697049.jpeg/orig",
    "//avatars.mds.yandex.net/get-mpic/4489193/img_id7820413109299165800.jpeg/orig",
    "//avatars.mds.yandex.net/get-mpic/3765589/img_id6658168663792038917.jpeg/orig",
    "//avatars.mds.yandex.net/get-mpic/4561793/img_id6270739844950654824.jpeg/orig",
]

AVAILABLE_SORT_TYPES = [
    EPromocodeSortType.PST_NO_SORT,
    EPromocodeSortType.PST_START_DATE_DESC,
    EPromocodeSortType.PST_END_DATE_ASC,
    EPromocodeSortType.PST_PERCENT_DISCOUNT_DESC,
    EPromocodeSortType.PST_ABSOLUTE_DISCOUNT_DESC,
]

SORTS_BY_NAME = {
    "no-sort": {
        "text": "Популярные",
        "options": [
            {
                "id": "no-sort",
                "isActive": False,
            },
        ],
    },
    "start-date-desc": {
        "text": "Сначала новые",
        "options": [
            {
                "id": "start-date-desc",
                "type": "desc",
                "isActive": False,
            },
        ],
    },
    "end-date-asc": {
        "text": "Скоро сгорят",
        "options": [
            {
                "id": "end-date-asc",
                "type": "asc",
                "isActive": False,
            },
        ],
    },
    "percent-discount-desc": {
        "text": "По скидке в %",
        "options": [
            {
                "id": "percent-discount-desc",
                "type": "desc",
                "isActive": False,
            },
        ],
    },
    "absolute-discount-desc": {
        "text": "По скидке в ₽",
        "options": [
            {
                "id": "absolute-discount-desc",
                "type": "desc",
                "isActive": False,
            },
        ],
    },
}


def create_promo(
    promo_id: str,
    image_count: int = len(PROMO_IMAGES),
    is_adult: bool = False,
    active_regions: list[int] = None,
    categories: list[int] = None,
    source: EPromoSource = EPromoSource.PS_PARTNER,
    discount_value: int = 500,
    discount_currency: str = 'RUB',
    start_date_delta=10000,
    end_date_delta=10000,
    sorts: list[EPromocodeSortType] = None,
) -> TPromo:
    default_sorts = [
        EPromocodeSortType.PST_NO_SORT,
        EPromocodeSortType.PST_START_DATE_DESC,
        EPromocodeSortType.PST_END_DATE_ASC,
        EPromocodeSortType.PST_ABSOLUTE_DISCOUNT_DESC,
        EPromocodeSortType.PST_PERCENT_DISCOUNT_DESC,
    ]
    return TPromo(
        discount_value=discount_value,
        discount_currency=discount_currency,
        start_date=int(datetime.now().timestamp() - start_date_delta),
        end_date=int(datetime.now().timestamp() + end_date_delta),
        code=f"PROMOCODE_{promo_id}",
        shop_promo_id=promo_id,
        source=source,
        source_promo_id="SoursePromoId",
        landing_url="https://market.yandex.ru/special/pampers-days",
        min_price=TPrice(value=3000, currency="RUB"),
        max_price=TPrice(value=15000, currency="RUB"),
        images=[TImage(id=10 + num, image=image_) for num, image_ in enumerate(PROMO_IMAGES[:image_count])],
        is_adult=is_adult,
        active_region=active_regions or [],
        categories=categories or [],
        sorts=sorts or default_sorts,
    )


class T(env.TestSuite):
    def get_promo_fragment(
        self,
        promo_id: str,
        image_count: int = len(PROMO_IMAGES),
        is_adult: bool = False,
        active_regions: "list[int]" = [],
        source: EPromoSource = EPromoSource.PS_PARTNER,
        categories: list[int] = [],
        discount_value: int = 500,
        discount_currency: str = 'RUB',
    ):
        return {
            "discountValue": f"{discount_value}",
            "discountCurrency": discount_currency,
            "code": f"PROMOCODE_{promo_id}",
            "shopPromoId": promo_id,
            "sourcePromoId": "SoursePromoId",
            "landingUrl": "https://market.yandex.ru/special/pampers-days",
            "minPrice": {"value": "3000", "currency": "RUB"},
            "maxPrice": {"value": "15000", "currency": "RUB"},
            "images": [{"id": str(10 + num), "image": image} for num, image in enumerate(PROMO_IMAGES[:image_count])],
            "isAdult": is_adult,
            "activeRegion": active_regions,
            "source": EPromoSource.Name(source),
            "categories": categories,
        }

    def get_sort(self, name: str, is_active: bool = False):
        sort = copy.deepcopy(SORTS_BY_NAME[name])
        sort["options"][0]["isActive"] = is_active
        return sort

    def get_sorts(self, names: list[str], active_sort: str):
        sorts = [(name, copy.deepcopy(sort)) for name, sort in SORTS_BY_NAME.items() if name in names]

        def set_active(sort):
            if sort[0] == active_sort:
                sort[1]["options"][0]["isActive"] = True
            return sort[1]

        return list(map(set_active, sorts))

    @classmethod
    def connect(cls):
        return {"dyno": cls.mars.dyno}

    @classmethod
    def prepare_promo_list(cls):
        # without sort
        cls.mars.dyno.add_promo_list(
            query=TPromoQuery(current_timestamp=2024, sort_type=EPromocodeSortType.PST_NO_SORT),
            promos=[
                create_promo(
                    promo_id="promo1",
                    start_date_delta=50000,
                    end_date_delta=10000,
                    discount_value=300,
                    source=EPromoSource.PS_PARTNER,
                ),
                create_promo(
                    promo_id="promo2",
                    start_date_delta=40000,
                    end_date_delta=20000,
                    discount_value=100,
                    source=EPromoSource.PS_MARKET,
                ),
            ],
            sorts=TSorts(sorts=AVAILABLE_SORT_TYPES),
        )

        # with sorts
        for index, sort_type in enumerate(AVAILABLE_SORT_TYPES, start=10):
            cls.mars.dyno.add_promo_list(
                query=TPromoQuery(current_timestamp=2025, sort_type=sort_type),
                promos=[
                    create_promo(
                        promo_id=f"promo{index}",
                        image_count=2,
                        start_date_delta=30000,
                        end_date_delta=30000,
                        discount_value=500,
                    ),
                ],
                sorts=TSorts(sorts=AVAILABLE_SORT_TYPES),
            )

        # with different sorts
        cls.mars.dyno.add_promo_list(
            query=TPromoQuery(current_timestamp=2026, sort_type=EPromocodeSortType.PST_NO_SORT),
            promos=[create_promo(promo_id="promo_lol")],
            sorts=TSorts(sorts=[EPromocodeSortType.PST_END_DATE_ASC, EPromocodeSortType.PST_ABSOLUTE_DISCOUNT_DESC]),
        )
        cls.mars.dyno.add_promo_list(
            query=TPromoQuery(current_timestamp=2027, sort_type=EPromocodeSortType.PST_NO_SORT),
            promos=[create_promo(promo_id="promo_kek")],
            sorts=TSorts(sorts=[EPromocodeSortType.PST_NO_SORT, EPromocodeSortType.PST_START_DATE_DESC]),
        )

        # with region
        cls.mars.dyno.add_promo_list(
            query=TPromoQuery(current_timestamp=2028, region=245, sort_type=EPromocodeSortType.PST_NO_SORT),
            promos=[create_promo(promo_id="promo_2")],
            sorts=TSorts(sorts=AVAILABLE_SORT_TYPES),
        )

    def test_promo_list_format(self):
        """Проверка формата выдачи"""
        response = self.mars.request_json('promo/list?current-timestamp=2024')
        self.assertFragmentIn(
            response,
            {
                "promoList": [
                    self.get_promo_fragment(promo_id="promo1", discount_value=300, source=EPromoSource.PS_PARTNER),
                    self.get_promo_fragment(promo_id="promo2", discount_value=100, source=EPromoSource.PS_MARKET),
                ],
                "sorts": self.get_sorts(
                    [
                        "no-sort",
                        "start-date-desc",
                        "end-date-asc",
                        "absolute-discount-desc",
                        "percent-discount-desc",
                    ],
                    active_sort="no-sort",
                ),
                "sources": [
                    "PS_MARKET",
                    "PS_PARTNER",
                ],
            },
            allow_different_len=False,
        )

    def test_min_promos_count(self):
        """Проверяем, что если промок недостаточно, то сформируется ошибка"""
        response = self.mars.request_json('promo/list?min-promo-count=3&current-timestamp=2024', fail_on_error=False)
        self.assertFragmentIn(response, {"errorMessage": Regex(".*not enough promos")})

    def test_pinned_sorts(self):
        """Проверяем, что в ответе пинится активная сортировка"""
        for index, (sort_type, how) in enumerate(zip(AVAILABLE_SORT_TYPES, SORTS_BY_NAME), start=10):
            response = self.mars.request_json(f'promo/list?current-timestamp=2025&how={how}')
            self.assertFragmentIn(
                response,
                {
                    "promoList": [
                        self.get_promo_fragment(promo_id=f"promo{index}", image_count=2, discount_value=500),
                    ],
                    "sorts": self.get_sorts(
                        [
                            "no-sort",
                            "start-date-desc",
                            "end-date-asc",
                            "absolute-discount-desc",
                            "percent-discount-desc",
                        ],
                        active_sort=how,
                    ),
                },
                preserve_order=True,
            )

    def test_sorts(self):
        """Проверяем, что сортировки приходят из промокодов из дино"""
        # нет сортировки, с которой мы запрашиваем
        response = self.mars.request_json('promo/list?current-timestamp=2026', fail_on_error=False)
        self.assertFragmentIn(response, {"errorMessage": Regex(".*unable to find sort 'no-sort'")})

        # есть сортировка, с которой мы запрашиваем
        response = self.mars.request_json('promo/list?current-timestamp=2027')
        self.assertFragmentIn(
            response,
            {
                "sorts": self.get_sorts(
                    [
                        "no-sort",
                        "start-date-desc",
                    ],
                    active_sort="no-sort",
                ),
            },
            allow_different_len=False,
        )

    def test_regions(self):
        """Проверяем, что запрашиваем различные регионы"""
        response = self.mars.request_json('promo/list?current-timestamp=2028&region=245')
        self.assertFragmentIn(
            response, {"promoList": [self.get_promo_fragment(promo_id="promo_2")]}, allow_different_len=False
        )


if __name__ == '__main__':
    env.main()
