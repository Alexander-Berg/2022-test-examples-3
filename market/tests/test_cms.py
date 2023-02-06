# -+- coding: utf-8 -+-

from getter.service import cms
from getter.validator import VerificationError

import unittest
from StringIO import StringIO


valid_source = '''{
    "info": {
        "version": "2018.11.12"
    },
    "result": [{
        "id": 123,
        "featured_msku": [{
            "msku": 789,
            "picture": "//avatars.mdt.yandex.net/766",
            "description": "Stroka s opisaniem"
        }, {
            "msku": 788,
            "picture": "//avatars.mdt.yandex.net/788",
            "description": "Prosto stroka"
        }],
        "force_relevance_msku": [{
            "msku": 7
        }, {
            "msku": 8
        }]
    }, {
        "id": 124,
        "force_relevance_msku": [{
            "msku": 17
        }, {
            "msku": 18
        }]
    }]
}'''

without_result = '''{
    "info": {
        "version": "2018.11.12"
    }
}'''

without_id = '''{
    "info": {
        "version": "2018.11.12"
    },
    "result": [{
        "featured_msku": [{
            "msku": 789,
            "picture": "//avatars.mdt.yandex.net/766",
            "description": "Stroka s opisaniem"
        }]
    }]
}'''

wrong_featured_msku__msku_missed = '''{
    "info": {
        "version": "2018.11.12"
    },
    "result": [{
        "id": 123,
        "featured_msku": [{
            "picture": "//avatars.mdt.yandex.net/766",
            "description": "Stroka s opisaniem"
        }]
    }]
}'''

wrong_featured_msku__msku_wrong_format = '''{
    "info": {
        "version": "2018.11.12"
    },
    "result": [{
        "id": 123,
        "featured_msku": [{
            "msku": "aaaA",
            "picture": "//avatars.mdt.yandex.net/766",
            "description": "Stroka s opisaniem"
        }]
    }]
}'''

wrong_featured_msku__without_picture = '''{
    "info": {
        "version": "2018.11.12"
    },
    "result": [{
        "id": 123,
        "featured_msku": [{
            "msku": 123,
            "description": "Stroka s opisaniem"
        }]
    }]
}'''

wrong_featured_msku__without_description = '''{
    "info": {
        "version": "2018.11.12"
    },
    "result": [{
        "id": 123,
        "featured_msku": [{
            "msku": 123,
            "picture": "//avatars.mdt.yandex.net/766"
        }]
    }]
}'''

wrong_force_relevance__without_msku = '''{
    "info": {
        "version": "2018.11.12"
    },
    "result": [{
        "id": 124,
        "force_relevance_msku": [{
            "__msku": 17
        }]
    }]
}'''

wrong_force_relevance___msku_format = '''{
    "info": {
        "version": "2018.11.12"
    },
    "result": [{
        "id": 124,
        "force_relevance_msku": [{
            "msku": "Aasd"
        }]
    }]
}'''


class Test(unittest.TestCase):
    def test_cms_validator(self):
        cms.validate_cms_promo(StringIO(valid_source))

        with self.assertRaises(VerificationError):
            cms.validate_cms_promo(StringIO(without_result))

        with self.assertRaises(VerificationError):
            cms.validate_cms_promo(StringIO(without_id))

        with self.assertRaises(VerificationError):
            cms.validate_cms_promo(StringIO(wrong_featured_msku__msku_missed))

        with self.assertRaises(VerificationError):
            cms.validate_cms_promo(StringIO(wrong_featured_msku__msku_wrong_format))

        with self.assertRaises(VerificationError):
            cms.validate_cms_promo(StringIO(wrong_featured_msku__without_picture))

        with self.assertRaises(VerificationError):
            cms.validate_cms_promo(StringIO(wrong_featured_msku__without_description))

        with self.assertRaises(VerificationError):
            cms.validate_cms_promo(StringIO(wrong_force_relevance__without_msku))

        with self.assertRaises(VerificationError):
            cms.validate_cms_promo(StringIO(wrong_force_relevance___msku_format))


if __name__ == '__main__':
    unittest.main()
