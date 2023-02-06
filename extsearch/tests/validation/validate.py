import os
import os.path

from lxml import etree
import yatest.common

CATALOG = yatest.common.source_path("maps/doc/schemas/catalog")
XSD = yatest.common.source_path("maps/doc/schemas/biz/advert/1.x")


def validate(xmlpath):
    schema = etree.XMLSchema(
        etree.fromstring(
            f'''
<schema xmlns="http://www.w3.org/2001/XMLSchema" version="1.0">
    <import namespace="http://www.w3.org/XML/1998/namespace" schemaLocation="{CATALOG}/xml.xsd"/>
    <import namespace="http://maps.yandex.ru/advert/1.x" schemaLocation="{XSD}/geoproduct.xsd"/>
</schema>'''
        )
    )
    xml = etree.parse(xmlpath)
    schema.validate(xml)

    return [f'{entry.filename}:{entry.line}:{entry.column}\n{entry.message}\n' for entry in schema.error_log]


def test__all_examples__validate():
    XMLDIR = yatest.common.source_path("extsearch/geo/indexer/advert_v2/tests/xml")

    files = []
    for dirpath, _, filenames in os.walk(XMLDIR):
        for filename in [f for f in filenames if f.endswith(".xml")]:
            files.append(os.path.join(dirpath, filename))

    assert len(files) > 4

    validation_errors = []
    for fi in files:
        validation_errors += validate(fi)

    assert len(validation_errors) == 0, "Errors, while validating:" + "\n".join(validation_errors)
