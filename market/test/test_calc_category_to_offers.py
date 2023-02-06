from market.idx.export.awaps.prepare_partition.lib.partition import calc_category_to_offers_num
import pytest

XML_AND_CATEGORY_TO_OFFERS_MAP = [
    {
        'xml': '''<?xml version="1.0" encoding="UTF-8"?>
                    <yml_catalog date="2020-02-25 18:13">
                        <shop>
                            <name>Yandex Market</name>
                            <company>Yandex</company>
                            <url>https://market.yandex.ru/</url>
                            <currencies>
                                <currency id="RUR" rate="1"/>
                            </currencies>
                            <categories>
                            <category id="90401">Все товары</category>
                            <category id="198118" parentId="90401">Бытовая техника</category>
                            <category id="198119" parentId="90401">Электроника</category>
                            <category id="17392030" parentId="90401">Упаковочные материалы для Беру</category>
                            <category id="90402" parentId="90401">Авто</category>
                            <category id="90509" parentId="90401">Товары для красоты</category>
                            <category id="90666" parentId="90401">Товары для дома</category>
                            <category id="90719" parentId="90401">Дача, сад и огород</category>
                            <category id="90764" parentId="90401">Детские товары</category>
                            <category id="90801" parentId="90401">Досуг и развлечения</category>
                            <category id="90813" parentId="90401">Товары для животных</category>
                            <category id="7877999" parentId="90401">Одежда, обувь и аксессуары</category>
                            </categories>
                            <offers>
                                <offer id="1" available="true">
                                    <url>https://beru.ru/product/shlang-palisad-professional-3-4-15-metrov/1977016902</url>
                                    <price>1029</price>
                                    <currencyId>RUR</currencyId>
                                    <categoryId>90401</categoryId>
                                    <picture>http://avatars.mds.yandex.net/get-mpic/1363210/img_id119081510555100058.jpeg/9</picture>
                                    <name>Шланг PALISAD Professional 3/4&quot; 15 метров</name>
                                    <vendor>PALISAD</vendor>
                                </offer>
                                <offer id="2" available="true">
                                    <url>https://beru.ru/product/shlang-palisad-professional-3-4-15-metrov/1977016902</url>
                                    <price>1029</price>
                                    <currencyId>RUR</currencyId>
                                    <categoryId>90401</categoryId>
                                    <picture>http://avatars.mds.yandex.net/get-mpic/1363210/img_id119081510555100058.jpeg/9</picture>
                                    <name>Шланг PALISAD Professional 3/4&quot; 15 метров</name>
                                    <vendor>PALISAD</vendor>
                                </offer>
                                <offer id="3" available="true">
                                    <url>https://beru.ru/product/shlang-palisad-professional-3-4-15-metrov/1977016902</url>
                                    <price>1029</price>
                                    <currencyId>RUR</currencyId>
                                    <categoryId>90403</categoryId>
                                    <picture>http://avatars.mds.yandex.net/get-mpic/1363210/img_id119081510555100058.jpeg/9</picture>
                                    <name>Шланг PALISAD Professional 3/4&quot; 15 метров</name>
                                    <vendor>PALISAD</vendor>
                                </offer>
                            </offers>
                        </shop>
                    </yml_catalog>''',
        'category_to_offers_map': {
            90401: 2,
            90403: 1
        }
    },
    {
        'xml': '''<?xml version="1.0" encoding="UTF-8"?>
                    <yml_catalog date="2020-02-25 18:13">
                        <shop>
                            <name>Yandex Market</name>
                            <company>Yandex</company>
                            <url>https://market.yandex.ru/</url>
                            <currencies>
                                <currency id="RUR" rate="1"/>
                            </currencies>
                            <offers>
                                <offer id="1" available="true">
                                    <url>https://beru.ru/product/shlang-palisad-professional-3-4-15-metrov/1977016902</url>
                                    <price>1029</price>
                                    <currencyId>RUR</currencyId>
                                    <picture>http://avatars.mds.yandex.net/get-mpic/1363210/img_id119081510555100058.jpeg/9</picture>
                                    <name>Шланг PALISAD Professional 3/4&quot; 15 метров</name>
                                    <vendor>PALISAD</vendor>
                                </offer>
                            </offers>
                        </shop>
                    </yml_catalog>''',
        'category_to_offers_map': {}
    },
    {
        'xml': '''<?xml version="1.0" encoding="UTF-8"?>
                    <yml_catalog date="2020-02-25 18:13">
                        <shop>
                            <name>Yandex Market</name>
                            <company>Yandex</company>
                            <url>https://market.yandex.ru/</url>
                            <currencies>
                                <currency id="RUR" rate="1"/>
                            </currencies>
                            <offers>
                            </offers>
                        </shop>
                    </yml_catalog>''',
        'category_to_offers_map': {}
    }
]


@pytest.mark.parametrize('o', XML_AND_CATEGORY_TO_OFFERS_MAP, ids=['allgood_ok', 'no_category_ok', 'no_offers_ok'])
def test_category_to_offers_map(o):
    xml = o['xml']
    category_to_offers_map = o['category_to_offers_map']
    assert calc_category_to_offers_num(xml) == category_to_offers_map
