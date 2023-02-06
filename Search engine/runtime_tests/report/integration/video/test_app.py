#!/usr/bin/env python2
# -*- coding: utf-8 -*-

"""
Тест для проверки ручек для приложения.
Таск: VIDEOUI-3483
"""

import pytest
import base
#import validictory     # альтернативный валидатор json. Делает очень строгие проверки.
import jsonschema       # Валидатор json. Делает "ленивые" проверки - например, отсутствие указанных в схеме ключей не проверяется.
import os


@pytest.mark.parametrize("domain", ["ru",])
@pytest.mark.parametrize("path", ["/app/?block=top", "/app/?block=popular", "/app/search?text=test"])
def test_app_morda(domain, path):
    url = base.make_url("yandex", domain, path=path)
    if path.endswith("test"):
        template = search
    else:
        template = blocks
    content = base.get_content(url)
    assert check(content, template)

def app_test_app(domain, path):
    """Тестовая функция"""
    url = base.make_url("yandex", domain, path=path)
    if path.endswith("test"):
        template = search
    else:
        template = blocks_test
    content = base.get_content(url)
    return check_test(content, template)

def check(c, t):
    try:
        jsonschema.validate(c, t)
    except jsonschema.ValidationError:
        return False
    else:
        return True

def check_test(c, t):
    """Тестовая функция"""
    try:
        jsonschema.validate(fake, t)
    except jsonschema.ValidationError:
        return False
    else:
        return True


search = {
  "$schema": "http://json-schema.org/draft-04/schema#",
  "description": "request example: http://yandex.ru/video/app/search?text=test",
  "type": "object",
  "properties": {
    "search_results": {
      "type": "object",
      "properties": {
        "error_code": {
          "type": "integer"
        },
        "found": {
          "type": "integer"
        },
        "clips": {
          "type": "array",
          "items": {
            "$ref": "file:{0}/report/integration/video/clip.json#".format(os.path.abspath(os.curdir))
          }
        },
        "navi": {
          "$ref": "file:{0}/report/integration/video/navi.json#".format(os.path.abspath(os.curdir))
        }
      },
      "required": [
        "error_code",
        "found",
        "clips",
        "navi"
      ]
    }
  },
  "required": ["search_results"]
}

blocks = {
  "$schema": "http://json-schema.org/draft-04/schema#",
  "description": "request example: http://yandex.ru/video/app/",
  "type": "object",
  "properties": {
    "blocks": {
      "type": "object",
      "properties": {
        "top": {
            "$ref": "file:{0}/report/integration/video/block.json#".format(os.path.abspath(os.curdir))
        },
        "favorites": {
            "$ref": "file:{0}/report/integration/video/block.json#".format(os.path.abspath(os.curdir))
        },
        "popular": {
            "$ref": "file:{0}/report/integration/video/popular.json#".format(os.path.abspath(os.curdir))
        }
      }
    }
  }
}

# Тестовая схема
blocks_test = {
  "$schema": "http://json-schema.org/draft-04/schema#",
  "description": "request example: http://yandex.ru/video/app/",
  "type": "object",
  "properties": {
    "blocks": {
      "type": "object",
      "properties": {
        "top": {
            "$ref": "file:{0}/block.json#".format(os.path.abspath(os.curdir))
        },
        "favorites": {
            "$ref": "file:{0}/block.json#".format(os.path.abspath(os.curdir))
        },
        "popular": {
            "$ref": "file:{0}/popular.json#".format(os.path.abspath(os.curdir))
        }
      }
    }
  }
}



if __name__ == "__main__":
    fake = {}   # file "fake.json"
    print app_test_app("ru", "/app/?block=top")
