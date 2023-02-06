# coding=utf-8

import json


sovetnik_only_wrong_json = """
[
    {
        "sovetnik": {
            "productPageSelector": "DVD проигрыватели NAD DVD проигрыватели других производителей Звоните"
        }
    }
]
"""

sovetnik_only_json = """
[
    {
        "sovetnik": {
            "attributes": {
                "category": "Проигрыватели и плееры",
                "name": "AV Ресивер NAD Viso-Five",
                "price": 123
            },
            "productPageSelector": "DVD проигрыватели NAD DVD проигрыватели других производителей Звоните"
        }
    }
]
"""

all_wrong_json = """
[
    {
        "market": {
            "available": null,
            "city": "$(document).ready(function(){ Ингушетия",
            "name": null,
            "picture": null,
            "price_final": null
        },
        "sovetnik": {
            "attributes": {
                "category": null,
                "name": null,
                "price": null,
                "vendor": null
            },
            "productPageSelector": null
        }
    }
]
"""
market_json = """
[
    {
        "market": {
            "category": "Категория",
            "city": "Город",
            "description": "Описание",
            "name": "Имя",
            "picture": "Картинка",
            "available": "true",
            "discounted": "true",
            "price_final": 123
        },
        "sovetnik": {
            "attributes": {
                "category": "Категория",
                "name": "Имя",
                "price": null
            },
            "productPageSelector": null
        }
    }
]
"""

all_json = """
[
    {
        "market": {
            "category": "Категория",
            "city": "Город",
            "description": "Описание",
            "name": "Имя",
            "picture": "Картинка",
            "price_final": 110,
            "price_discount": 100,
            "price_before_discount": 123
        },
        "sovetnik": {
            "attributes": {
                "category": "Категория",
                "name": "Имя",
                "price": 123
            },
            "productPageSelector": null
        }
    }
]
"""

all_json_with_barcode = """
[
    {
        "market": {
            "category": "Категория",
            "city": "Город",
            "description": "Описание",
            "name": "Имя",
            "picture": "Картинка",
            "barcode": "12312312",
            "price_final": 110,
            "price_discount": 100,
            "price_before_discount": 123
        },
        "sovetnik": {
            "attributes": {
                "category": "Категория",
                "name": "Имя",
                "price": 123
            },
            "productPageSelector": null
        }
    }
]
"""

null_in_category_json = """
[
    {
        "market": {
            "category": null,
            "city": "Город",
            "description": "Описание",
            "name": "Имя",
            "picture": "Картинка",
            "price_before_discount": 123
        },
        "sovetnik": {
            "attributes": {
                "category": "Категория",
                "name": "Имя",
                "price": 123
            },
            "productPageSelector": null
        }
    }
]
"""

with_watson_json = """
[
    {
        "market" : {
            "available" : null,
            "category" : "Женская обувь",
            "city" : "Москва",
            "description" : "Описание",
            "discounted" : null,
            "name" : "Угги Patrol",
            "color": "красный",
            "picture" : null,
            "price_before_discount" : null,
            "price_discount" : null,
            "price_final" : "123 Р"
        },
        "sovetnik" : {
            "attributes" : {
                "category" : "Одежда, обувь и аксессуары",
                "isbn" : "Угги Patrol — купить в интернет-магазине OZON.ru с быстрой доставкой",
                "name" : "Угги Patrol",
                "price" : null
            },
            "categoryPageSelector" : null,
            "productPageSelector" : "Узнать о поступлении"
        },
        "watson" : {
            "description" : "Описание",
            "discount" : null,
            "other_products" : [
                {
                    "discount" : "11 190₽",
                    "price" : "4 476₽",
                    "title" : "Угги Winzor",
                    "url" : "https://www.ozon.ru/context/detail/id/147047548/"
                },
                {
                    "discount" : "9 690₽",
                    "price" : "3 876₽",
                    "title" : "Угги Vitacci",
                    "url" : "https://www.ozon.ru/context/detail/id/146685551/"
                },
                {
                    "discount" : null,
                    "price" : "6 250₽",
                    "title" : "Угги Graciana",
                    "url" : "https://www.ozon.ru/context/detail/id/143539115/"
                }
            ],
            "price" : "",
            "title" : "Угги Patrol"
        }
    }
]
"""

with_watson_wrong_market = """
[
    {
        "market" : {
            "available" : null,
            "category" : "Женская обувь",
            "city" : "Москва",
            "description" : "Описание",
            "discounted" : null,
            "name" : "",
            "color": "красный",
            "picture" : null,
            "price_before_discount" : null,
            "price_discount" : null,
            "price_final" : "123 Р"
        },
        "sovetnik" : {
            "attributes" : {
                "category" : "Одежда, обувь и аксессуары",
                "isbn" : "Угги Patrol — купить в интернет-магазине OZON.ru с быстрой доставкой",
                "name" : "Угги Patrol",
                "price" : null
            },
            "categoryPageSelector" : null,
            "productPageSelector" : "Узнать о поступлении"
        },
        "watson" : {
            "description" : "Описание",
            "discount" : null,
            "other_products" : [
                {
                    "discount" : "11 190₽",
                    "price" : "4 476₽",
                    "title" : "Угги Winzor",
                    "url" : "https://www.ozon.ru/context/detail/id/147047548/"
                },
                {
                    "discount" : "9 690₽",
                    "price" : "3 876₽",
                    "title" : "Угги Vitacci",
                    "url" : "https://www.ozon.ru/context/detail/id/146685551/"
                },
                {
                    "discount" : null,
                    "price" : "6 250₽",
                    "title" : "Угги Graciana",
                    "url" : "https://www.ozon.ru/context/detail/id/143539115/"
                },
                {
                    "discount" : "7 769₽",
                    "price" : "3 108₽",
                    "title" : "Угги GOGC",
                    "url" : "https://www.ozon.ru/context/detail/id/147739312/"
                },
                {
                    "discount" : "7 900₽",
                    "price" : "3 916₽",
                    "title" : "Угги Bearpaw",
                    "url" : "https://www.ozon.ru/context/detail/id/147177317/"
                },
                {
                    "discount" : "5 890₽",
                    "price" : "2 356₽",
                    "title" : "Угги Vitacci",
                    "url" : "https://www.ozon.ru/context/detail/id/146684458/"
                }
            ],
            "price" : "123 Р",
            "title" : "Угги Patrol",
            "color": "красный"
        }
    }
]
"""

json_with_params = """
[
    {
        "market" : {
            "available" : null,
            "category" : "Отделочные и строительные материалы",
            "description" : null,
            "name" : "Обои декоративные Rasch, Kids & Teens, 740080 (0,53x10м)",
            "not_available" : "",
            "param_name" : [
                "Коллекция",
                "Тип",
                "Ширина рулона, см",
                "Основной цвет"
            ],
            "param_value" : [
                "Kids & Teens",
                "Декоративные",
                "53",
                "Синий-Голубой"
            ],
            "picture" : "https://p.fast.ulmart.ru/p/mid/356/35665/3566513.jpg",
            "price_before_discount" : null,
            "price_discount" : "—",
            "price_final" : "123"
        },
        "sovetnik" : {
            "attributes" : {
                "category" : "Дом, дача и зоотовары",
                "name" : "Обои декоративные Rasch, Kids & Teens, 740080 (0,53x10м)",
                "price" : "—"
            },
            "categoryPageSelector" : null,
            "productPageSelector" : ""
        }
    }
]
"""

json_available = """
[
    {
        "market" : {
            "available": "Товар временно отсутствует в продаже",
            "category": "Духовые шкафы",
            "city": "Москва",
            "description": "36 639 р.",
            "name": "Встраиваемая газовая духовка ELECTROLUX EOG91102AX",
            "param_name": [],
            "param_value" : [],
            "picture": "https://cdnmedia.220-volt.ru/content/products/255/255677/images/thumb_220/n1200x800_q80/1.jpeg",
            "price_before_discount" : "36 639 р.",
            "price_discount" : "35 919",
            "price_final" : "35 919"
        },
        "sovetnik" : {
            "attributes" : {
                "category" : "ELECTROLUX",
                "name" : "Встраиваемая газовая духовка ELECTROLUX EOG91102AX",
                "price" : "35 919"
            },
            "categoryPageSelector" : "",
            "productPageSelector" : "Цена в розничной сети: 36 639 р. Товар временно отсутствует в продаже Последняя цена: 35 919 р. Сообщить о поступлении Смотреть аналоги"
        }
    }
]
"""

json_not_available = """
[
    {
        "market" : {
            "category": "Стилусы",
            "city": "Москва",
            "color": "",
            "description" : "Материал корпуса кожа Цвет розовый",
            "discounted" : "Финальная цена",
            "name" : "Чехол для стилуса Apple Pencil Case Soft Pink",
            "not_available" : "Финальная цена",
            "param_name" : [],
            "param_value" : [],
            "picture" : "",
            "price_before_discount" : "1 990¤",
            "price_discount" : "1 790¤",
            "price_final" : "1 790¤"
        },
        "sovetnik" : {
            "attributes" : {
                "category" : "Аксессуары для планшетов",
                "name" : "Чехол для стилуса Apple Pencil Case Soft Pink",
                "price" : "1 790¤"
            },
            "categoryPageSelector" : ""
        }
    }
]
"""

json_discounted = """
[
    {
        "market" : {
            "category" : "Стилусы",
            "city" : "Москва",
            "color" : "",
            "description" : "Материал корпуса кожа Цвет синий",
            "discounted" : "Витринный образец",
            "name" : "Чехол для стилуса Apple Pencil Case Electric Blue",
            "not_available" : "Финальная цена",
            "param_name" : [],
            "param_value" : [],
            "picture" : "",
            "price_before_discount" : "1 990¤",
            "price_discount" : "1 790¤",
            "price_final" : "1 790¤"
        },
        "sovetnik" : {
            "attributes" : {
                "category" : "Аксессуары для планшетов",
                "name" : "Чехол для стилуса Apple Pencil Case Electric Blue",
                "price" : "1 790¤"
            },
            "categoryPageSelector" : ""
        }
    }
]
"""


class SovetnikElem:
    def __init__(self, name=None, price=None, set_name=True, set_price=True):
        if set_name:
            self.name = name
        if set_price:
            self.price = price

    def to_json(self):
        return json.dumps([{"sovetnik": {"attributes": self.__dict__}}])


class MarketElem:
    def __init__(self, category=None, city=None, description=None, picture=None, available=None,
                 discounted=None, price_before_discount=None, price_discount=None, price_final=None, name=None,
                 set_name=True, set_price_final=True, set_price_discount=True, set_price_before_discount=True):
        self.category = category
        self.city = city
        self.description = description
        self.picture = picture
        self.available = available
        self.discounted = discounted
        if set_name:
            self.name = name
        if set_price_final:
            self.price_final = price_final
        if set_price_before_discount:
            self.price_before_discount = price_before_discount
        if set_price_discount:
            self.price_discount = price_discount

    def to_json(self):
        return json.dumps([{"market": self.__dict__}])


class NormalWatsonElem:
    def __init__(self, category=None, city=None, description=None, picture=None, available=None,
                 discounted=None, price_before_discount=None, price_discount=None, price_final=None, name=None,
                 set_name=True, set_price_final=True, set_price_discount=True, set_price_before_discount=True,
                 other_products=None):
        self.category = category
        self.city = city
        self.description = description
        self.picture = picture
        self.available = available
        self.discounted = discounted
        self.other_products = other_products
        if set_name:
            self.name = name
        if set_price_final:
            self.price_final = price_final
        if set_price_before_discount:
            self.price_before_discount = price_before_discount
        if set_price_discount:
            self.price_discount = price_discount

    def to_json(self):
        return json.dumps([{"watson": self.__dict__}])


class NormalWatsonElemWithRecommendations:
    def __init__(self, normal_watson_elem, other_products):
        self.category = normal_watson_elem.category
        self.city = normal_watson_elem.city
        self.description = normal_watson_elem.description
        self.picture = normal_watson_elem.picture
        self.available = normal_watson_elem.available
        self.discounted = normal_watson_elem.discounted
        self.other_products = other_products
        if hasattr(normal_watson_elem, "name"):
            self.name = normal_watson_elem.name
        if hasattr(normal_watson_elem, "price_final"):
            self.price_final = normal_watson_elem.price_final
        if hasattr(normal_watson_elem, "price_before_discount"):
            self.price_before_discount = normal_watson_elem.price_before_discount
        if hasattr(normal_watson_elem, "price_discount"):
            self.price_discount = normal_watson_elem.price_discount
        self.other_products = other_products

    @classmethod
    def create_dummy(cls, normal_watson_elem):
        return cls(normal_watson_elem=normal_watson_elem, other_products=[normal_watson_elem.__dict__])

    def to_json(self):
        return json.dumps([{"watson": self.__dict__}])


class WatsonElem:
    def __init__(self, description=None, discount=None, price=None, title=None,
                 set_title=True, set_price=True, set_discount=True, set_description=True):
        if set_title:
            self.title = title
        if set_price:
            self.price = price
        if set_discount:
            self.discount = discount
        if set_description:
            self.description = description

    def to_json(self):
        return json.dumps([{"watson": self.__dict__}])


class WatsonElemWithRecommendations:
    def __init__(self, watson_elem, other_products):
        if hasattr(watson_elem, "title"):
            self.title = watson_elem.title
        if hasattr(watson_elem, "price"):
            self.price = watson_elem.price
        if hasattr(watson_elem, "discount"):
            self.discount = watson_elem.discount
        if hasattr(watson_elem, "description"):
            self.description = watson_elem.description
        self.other_products = other_products

    def to_json(self):
        return json.dumps([{"watson": self.__dict__}])
