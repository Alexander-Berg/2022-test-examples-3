from lxml import etree


def factory_xml_flight(faker, node_code, **attrib):
    default = dict(
        NAT=faker.pystr(),
        CANCELLED='1',
    )

    attrib = {**default, **attrib}

    return etree.Element(node_code, **attrib)
