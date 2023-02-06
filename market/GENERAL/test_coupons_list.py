import market.mars.lite.env as env

from market.mars.lib.promo.coupons.proto.coupons_pb2 import TCouponsData, TCouponsListItemInfo

columns_count = 11
coupons_names_anon = list(map(str, range(1, columns_count + 1)))
coupons_names = [
    "All_246125",
    "Beauty_246121",
    "CEHAC_246485",
    "DIY_HOME_246123",
    "Ecom_263493",
    "Fashion_246116",
    "Flowers_247586",
    "Kids_246115",
    "Kids_246305",
    "Pets_246304",
    "Sport_246118",
]


def create_coupon(coupons_count: int) -> dict[str, int]:
    return {f'{coupons_names[i]}': int(i < coupons_count) for i in range(columns_count)}


def create_coupon_data(puid: str, coupons_count: int) -> TCouponsData:
    return TCouponsData(
        Puid=puid,
        CouponsCount=coupons_count,
        Coupons=create_coupon(coupons_count),
    )


def create_coupons_list_info(name: str, title: str, picUrl: str) -> TCouponsListItemInfo:
    return TCouponsListItemInfo(
        Name=name,
        Title=title,
        PictureUrl=picUrl,
    )


class T(env.TestSuite):
    @classmethod
    def connect(cls):
        return {"dyno": cls.mars.dyno}

    @classmethod
    def prepare_coupons_list(cls):
        cls.mars.dyno.add_coupon(
            proto_message=create_coupon_data(puid="611", coupons_count=3),
            puid="611",
        )
        cls.mars.dyno.add_coupons_list_info(
            proto_message=create_coupons_list_info("All_246125", "all", "url"),
            name="All_246125",
        )
        cls.mars.dyno.add_coupons_list_info(
            proto_message=create_coupons_list_info("Beauty_246121", "beauty", "url"),
            name="Beauty_246121",
        )
        cls.mars.dyno.add_coupons_list_info(
            proto_message=create_coupons_list_info("CEHAC_246485", "cehac", "url"),
            name="CEHAC_246485",
        )
        cls.mars.dyno.add_empty_response_to_coupon(puid="1007")

    def test_right_response(self):
        """
        Проверяем, что при количестве купонов превыщающем minCount возвращаем все купоны
        """
        response = self.mars.request_json('promo/coupon/list?puid=611&source=GO&client=homeplus&minCount=0')
        self.assertFragmentIn(
            response,
            {
                "items": [
                    {"id": "All_246125", "title": "all", "picture": "url", "url": "https://link"},
                    {"id": "Beauty_246121", "title": "beauty", "picture": "url", "url": "https://link"},
                    {"id": "CEHAC_246485", "title": "cehac", "picture": "url", "url": "https://link"},
                ]
            },
        )

        response = self.mars.request_json('promo/coupon/list?puid=611&source=GO&client=homeplus&minCount=2')
        self.assertFragmentIn(
            response,
            {
                "items": [
                    {"id": "All_246125", "title": "all", "picture": "url", "url": "https://link"},
                    {"id": "Beauty_246121", "title": "beauty", "picture": "url", "url": "https://link"},
                    {"id": "CEHAC_246485", "title": "cehac", "picture": "url", "url": "https://link"},
                ]
            },
        )

        response = self.mars.request_json('promo/coupon/list?puid=611&source=GO&client=homeplus&minCount=3')
        self.assertFragmentIn(
            response,
            {
                "items": [
                    {"id": "All_246125", "title": "all", "picture": "url", "url": "https://link"},
                    {"id": "Beauty_246121", "title": "beauty", "picture": "url", "url": "https://link"},
                    {"id": "CEHAC_246485", "title": "cehac", "picture": "url", "url": "https://link"},
                ]
            },
        )

    def test_not_enough_coupons(self):
        response = self.mars.request_json('promo/coupon/list?puid=611&source=GO&client=homeplus&minCount=4')
        self.assertFragmentIn(response, {"items": []})

        response = self.mars.request_json('promo/coupon/list?puid=1007&source=GO&client=homeplus&minCount=0')
        self.assertFragmentIn(response, {"items": []})

    def test_coupons_list_not_enough_coupons_metrics(self):
        # не хватает купонов
        self.mars.request_json('promo/coupon/list?puid=611&source=GO&client=homeplus&minCount=4')
        stat_respones = self.mars.request_json('stat')
        self.assertFragmentIn(stat_respones, ['mpage=homeplus;mservice=GO;couponsList_not_enough_coupons_dmmm', 1])

        # puid-а нет в таблице (считаем, что couponsCount = 0)
        self.mars.request_json('promo/coupon/list?puid=1007&source=GO&client=homeplus&minCount=0')
        stat_respones = self.mars.request_json('stat')
        self.assertFragmentIn(stat_respones, ['mpage=homeplus;mservice=GO;couponsList_not_enough_coupons_dmmm', 2])

        self.assertFragmentIn(
            stat_respones, ['mpage=homeplus;mservice=GO;couponsList_dyno_response_process_time_hgram']
        )

        self.mars.request_text("stat/reset", "POST")


if __name__ == '__main__':
    env.main()
