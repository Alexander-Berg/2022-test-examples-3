# -*- coding: utf-8 -*-
"""Бонусы по продуктам."""
from collections import namedtuple

from mpfs.config import settings

from test.helpers.size_units import GB


Product = namedtuple("Product", ["id", "amount"])


APP_INSTALL = Product("app_install", 3*GB)
BLAT_250 = Product("blat_250", settings.extra_space['blat250']['value'])
CLIENT = Product("client", settings.extra_space['client']['value'])
FILE_UPLOADED = Product("file_uploaded", settings.extra_space['upload']['value'])
INITIAL_10GB = Product("initial_10gb", 10*GB)
INITIAL_5GB = Product("initial_5gb", 5*GB)
INITIAL_3GB = Product("initial_3gb", 3*GB)
PASSPORT_SPLIT = Product("passport_split", settings.extra_space['passportsplit']['value'])
PROMO_SHARED = Product("promo_shared", settings.extra_space['promo']['value'])
SONY_NOTEBOOK = Product("sony_notebook", 30*GB)
SONY_TABLET = Product("sony_tablet", 40*GB)
TURKEY_PANORAMA = Product("turkey_panorama", settings.extra_space['turkeypanorama']['value'])
TURKEY_PROJE_Y = Product("turkey_proje_y", settings.extra_space['turkeyprojey']['value'])
TURKISH_USER = Product("turkish_user", 1*GB)
YANDEX_BROWSER = Product("yandex_browser", settings.extra_space['yandexbrowser']['value'])
YANDEX_EGE = Product("yandex_ege", settings.extra_space['yandexege']['value'])
YANDEX_SHAD = Product("yandex_shad", settings.extra_space['yandexshad']['value'])
YANDEX_STAFF = Product("yandex_staff", settings.extra_space['yandexstaff']['value'])
YANDEX_PLUS = Product("yandex_plus_10gb", 10*GB)
