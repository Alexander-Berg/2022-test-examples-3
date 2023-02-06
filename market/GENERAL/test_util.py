import base64
import market.mars.lite.env as env

from yabs.proto.user_profile_pb2 import Profile


class T(env.TestSuite):
    @classmethod
    def prepare(cls):
        cls.bigb = cls.mars.bigb_pg

    def _test_bibg(self, hyperids):
        profile = self.bigb.gen_profile(hyperids)
        bigb = base64.b64decode(profile)
        data = Profile()
        data.ParseFromString(bigb)
        for profile in data.dj_profiles:
            erfs = profile.profile.Erfs
            self.assertTrue(len(erfs) > 0)

    def test_generate_bigb_profile_commondata(self):
        """Проверяем что генератор профилей bigb выдаёт ожидаемый релузьтат"""
        # Моделей для пользователя нет
        self._test_bibg([])
        # Есть одна модель с нулевым id
        self._test_bibg([0])
        # Есть модели с не нулевыми id
        self._test_bibg([1, 2])
        # Есть несколько моделей
        self._test_bibg([1, 2, 12, 212, 12212])


if __name__ == '__main__':
    env.main()
