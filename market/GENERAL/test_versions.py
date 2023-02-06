from market.content_storage_service.lite.core.testcase import TestCase, main
from market.pylibrary.lite.matcher import NotEmpty


class T(TestCase):
    def test_versions(self):
        response = self.market_content_storage.request_json('versions', method='GET')
        self.assertFragmentIn(response, {
            "MboData": NotEmpty(),
            "PersData": NotEmpty(),
            "RecomData": NotEmpty(),
            "NaviDataTs": NotEmpty(),
        })


if __name__ == '__main__':
    main()
