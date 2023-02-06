# -+- coding: utf-8 -+-

from getter.service import cms_business_logos
# from getter.validator import VerificationError

import unittest
import json
from StringIO import StringIO


valid_source = '''{
    "__info__": {
        "version":"2021.10.27"
    },
    "result": [{
        "businesses": [{
            "businessId":2222,
            "brandColor":"#134727",
            "shopGroup": "vo_dvore",
            "logos": [{
                "type":"rectangle",
                "url":"//avatars.mdst.yandex.net/get-marketcms/69442/img-dc9a8f73-2945-4af9-a029-8838122d2a5a.svg/orig",
                "entity":"picture",
                "width":"1288",
                "height":"344"
            },
            {
                "type":"square",
                "url":"//avatars.mdst.yandex.net/get-marketcms/69442/img-48516751-8cf6-4e8d-b4ea-1d99a5a2584d.svg/orig",
                "entity":"picture",
                "width":"40",
                "height":"32"
            }]
        },
        {
            "businessId":1111,
            "brandColor":"#CB121D",
            "logos": [{
                "type":"rectangle",
                "url":"//avatars.mdst.yandex.net/get-marketcms/69555/img-0c646b8f-28aa-4c89-9eb0-6944e3fc6dac.svg/orig",
                "entity":"picture",
                "width":"951",
                "height":"181"
            },
            {
                "type":"square",
                "url":"//avatars.mdst.yandex.net/get-marketcms/69555/img-d8980eee-813a-47ac-ab1f-f03e491be96c.svg/orig",
                "entity":"picture",
                "width":"31",
                "height":"31"
            }]
        },
        {
            "businessId":3333,
            "brandColor":"#2DBE64",
            "shopGroup":"gipermarket"
        },
        {
            "businessId":4444,
            "logos": [{
                "type":"rectangle",
                "url":"//avatars.mdst.yandex.net/get-marketcms/69442/img-34a2e1c8-aa9a-41aa-9cc0-bb899dfa30af.svg/orig",
                "entity":"picture",
                "width":"1512",
                "height":"404"
            },
            {
                "type":"square",
                "url":"//avatars.mdst.yandex.net/get-marketcms/69442/img-3070f347-0191-4846-90b0-a237970e481b.svg/orig",
                "entity":"picture",
                "width":"50",
                "height":"24"
            }]
        }],
        "type":"report_business_logos"
    }]
}'''

json_sample = '''{
  "Business": [
    {
      "BusinessId": 1111,
      "Logos": [
        {
          "LogoType": "rectangle",
          "Namespace": "marketcms",
          "GroupId": "69555",
          "Key": "img-0c646b8f-28aa-4c89-9eb0-6944e3fc6dac.svg",
          "Width": 951,
          "Height": 181
        },
        {
          "LogoType": "square",
          "Namespace": "marketcms",
          "GroupId": "69555",
          "Key": "img-d8980eee-813a-47ac-ab1f-f03e491be96c.svg",
          "Width": 31,
          "Height": 31
        }
      ],
      "BrandColor": "#CB121D"
    },
    {
      "BusinessId": 2222,
      "Logos": [
        {
          "LogoType": "rectangle",
          "Namespace": "marketcms",
          "GroupId": "69442",
          "Key": "img-dc9a8f73-2945-4af9-a029-8838122d2a5a.svg",
          "Width": 1288,
          "Height": 344
        },
        {
          "LogoType": "square",
          "Namespace": "marketcms",
          "GroupId": "69442",
          "Key": "img-48516751-8cf6-4e8d-b4ea-1d99a5a2584d.svg",
          "Width": 40,
          "Height": 32
        }
      ],
      "BrandColor": "#134727",
      "ShopGroup": "vo_dvore"
    },
    {
      "BusinessId": 3333,
      "BrandColor": "#2DBE64",
      "ShopGroup": "gipermarket"
    },
    {
      "BusinessId": 4444,
      "Logos": [
        {
          "LogoType": "rectangle",
          "Namespace": "marketcms",
          "GroupId": "69442",
          "Key": "img-34a2e1c8-aa9a-41aa-9cc0-bb899dfa30af.svg",
          "Width": 1512,
          "Height": 404
        },
        {
          "LogoType": "square",
          "Namespace": "marketcms",
          "GroupId": "69442",
          "Key": "img-3070f347-0191-4846-90b0-a237970e481b.svg",
          "Width": 50,
          "Height": 24
        }
      ]
    }
  ]
}'''

without_result = '''{
    "info": {
        "version": "2018.11.12"
    }
}'''

without_business_id = '''{
    "__info__": {
        "version":"2021.10.27"
    },
    "result": [{
        "businesses": [{
            "brandColor":"#134727",
            "logos": [{
                "type":"rectangle",
                "url":"//avatars.mdst.yandex.net/get-marketcms/69442/img-dc9a8f73-2945-4af9-a029-8838122d2a5a.svg/orig",
                "entity":"picture",
                "width":"1288",
                "height":"344"
            }
        }],
        "type":"report_business_logos"
    }]
}'''

without_type = '''{
    "__info__": {
        "version":"2021.10.27"
    },
    "result": [{
        "businesses": [{
            "businessId":2222,
            "brandColor":"#134727",
            "logos": [{
                "url":"//avatars.mdst.yandex.net/get-marketcms/69442/img-dc9a8f73-2945-4af9-a029-8838122d2a5a.svg/orig",
                "entity":"picture",
                "width":"1288",
                "height":"344"
            }
        }],
        "type":"report_business_logos"
    }]
}'''

without_url = '''{
    "__info__": {
        "version":"2021.10.27"
    },
    "result": [{
        "businesses": [{
            "businessId":2222,
            "brandColor":"#134727",
            "logos": [{
                "type":"rectangle",
                "entity":"picture",
                "width":"1288",
                "height":"344"
            }
        }],
        "type":"report_business_logos"
    }]
}'''

without_entity = '''{
    "__info__": {
        "version":"2021.10.27"
    },
    "result": [{
        "businesses": [{
            "businessId":2222,
            "brandColor":"#134727",
            "logos": [{
                "type":"rectangle",
                "url":"//avatars.mdst.yandex.net/get-marketcms/69442/img-dc9a8f73-2945-4af9-a029-8838122d2a5a.svg/orig",
                "width":"1288",
                "height":"344"
            }
        }],
        "type":"report_business_logos"
    }]
}'''

without_width = '''{
    "__info__": {
        "version":"2021.10.27"
    },
    "result": [{
        "businesses": [{
            "businessId":2222,
            "brandColor":"#134727",
            "logos": [{
                "type":"rectangle",
                "url":"//avatars.mdst.yandex.net/get-marketcms/69442/img-dc9a8f73-2945-4af9-a029-8838122d2a5a.svg/orig",
                "entity":"picture",
                "height":"344"
            }
        }],
        "type":"report_business_logos"
    }]
}'''

without_height = '''{
    "__info__": {
        "version":"2021.10.27"
    },
    "result": [{
        "businesses": [{
            "businessId":2222,
            "brandColor":"#134727",
            "logos": [{
                "type":"rectangle",
                "url":"//avatars.mdst.yandex.net/get-marketcms/69442/img-dc9a8f73-2945-4af9-a029-8838122d2a5a.svg/orig",
                "entity":"picture",
                "width":"1288"
            }
        }],
        "type":"report_business_logos"
    }]
}'''


def jsons_equal(json1, json2):
    def recursive_sort(obj):
        if isinstance(obj, dict):
            return sorted((k, recursive_sort(v)) for k, v in obj.items())
        if isinstance(obj, list):
            return sorted(recursive_sort(x) for x in obj)
        else:
            return obj

    return recursive_sort(json1) == recursive_sort(json2)


class Test(unittest.TestCase):
    def test_cms_business_logos_validator(self):
        cms_business_logos.validate_cms_business_logos(StringIO(valid_source))

        '''
        Включить при запуске проекта
        https://st.yandex-team.ru/MARKETOUT-45341

        with self.assertRaises(VerificationError):
            cms_business_logos.validate_cms_business_logos(StringIO(without_result))

        with self.assertRaises(VerificationError):
            cms_business_logos.validate_cms_business_logos(StringIO(without_business_id))

        with self.assertRaises(VerificationError):
            cms_business_logos.validate_cms_business_logos(StringIO(without_type))

        with self.assertRaises(VerificationError):
            cms_business_logos.validate_cms_business_logos(StringIO(without_url))

        with self.assertRaises(VerificationError):
            cms_business_logos.validate_cms_business_logos(StringIO(without_entity))

        with self.assertRaises(VerificationError):
            cms_business_logos.validate_cms_business_logos(StringIO(without_width))

        with self.assertRaises(VerificationError):
            cms_business_logos.validate_cms_business_logos(StringIO(without_height))
        '''

    def templator_business_logo_json_converter(self):
        templator_json = json.loads(valid_source)
        converted_json = cms_business_logos.templator_business_logo_json_converter(templator_json)
        assert jsons_equal(converted_json, json_sample), "templator_business_logo_json_converter returns wrong data"

if __name__ == '__main__':
    unittest.main()
