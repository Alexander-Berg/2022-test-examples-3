import market.mars.lite.env as env

from market.mars.lib.promo.coupons.proto.coupons_pb2 import TCouponsData

columns_count = 11
coupons_names_anon = list(map(str, range(1, columns_count + 1)))


def create_coupon(coupons_count: int) -> dict:
    coupons_column_ones_ind = list(map(int, range(coupons_count)))
    return {f'{coupons_names_anon[i]}': (1 if i in coupons_column_ones_ind else 0) for i in range(columns_count)}


def create_coupon_data(puid: str, couponsCount: int) -> TCouponsData:
    return TCouponsData(
        Puid=puid,
        CouponsCount=couponsCount,
        Coupons=create_coupon(couponsCount),
    )


class T(env.TestSuite):
    @classmethod
    def connect(cls):
        return {'dyno': cls.mars.dyno}

    @classmethod
    def prepare_data_with_coupons(cls):
        cls.mars.dyno.add_coupon(
            proto_message=create_coupon_data(puid="10101", couponsCount=1),
            puid="10101",
        )
        cls.mars.dyno.add_empty_response_to_coupon(puid="1")

    def test_coupons_right_response(self):
        response = self.mars.request_json('promo/coupon?puid=10101&client=homeplus&source=GO')
        self.assertFragmentIn(
            response, {"coupon": {"imageUrl": "avatars.mds.yandex.net/get-mpic/5234357/cehac-v2/12hq"}}
        )

    def test_coupons_not_in_table_puid(self):
        response = self.mars.request_json('promo/coupon?puid=1&client=homeplus&source=GO', fail_on_error=False)
        self.assertFragmentIn(response, {})

    @classmethod
    def prepare_data_without_coupons(cls):
        cls.mars.dyno.add_coupon(
            proto_message=create_coupon_data(puid="1111", couponsCount=0),
            puid="1111",
        )

    def test_coupons_empty_response(self):
        response = self.mars.request_json('promo/coupon?puid=1111&client=abra&source=cadabra')
        self.assertFragmentIn(response, {})


if __name__ == '__main__':
    env.main()
