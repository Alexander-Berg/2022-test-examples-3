import io

import pytest
from freezegun import freeze_time

from travel.avia.ad_feed.ad_feed.feed_generator.wizard_like import persistent_string_hash
from travel.avia.ad_feed.ad_feed.converter.xml_converter import Category
from travel.avia.ad_feed.ad_feed.converter import XmlYandexSmartBannersConverter


@pytest.fixture()
def converter() -> XmlYandexSmartBannersConverter[dict[str, str]]:
    return XmlYandexSmartBannersConverter[dict[str, str]](
        mapper={'x': lambda row: row['x']},
        category_extractor=lambda row: Category(id=persistent_string_hash(row['x']), name=row['x']),
    )


@freeze_time("2021-06-11 11:20")
def test_xml_converter(converter: XmlYandexSmartBannersConverter[dict[str, str]]):
    output = io.BytesIO()
    converter.convert([{'x': 'hello'}, {'x': 'kak dela?'}], output=output)
    assert (
        output.getvalue().decode('utf-8') == '<?xml version=\'1.0\' encoding=\'utf-8\'?>\n'
        '<avia-feed><generation-date>2021-06-11T11:20</generation-date>'
        '<categories><category id="977371461">hello</category>'
        '<category id="135158535">kak dela?</category></categories>'
        '<offers><offer id="0"><x>hello</x></offer><offer id="1"><x>kak dela?</x></offer></offers>'
        '</avia-feed>'
    )
