# -*- coding: utf-8 -*-

import pytest
from lxml import etree

from cysix.filters.cysix_xml_validation import check_transports
from travel.rasp.admin.lib.logs import get_collector_context


def check_transports_messages(raw_xml):
    with get_collector_context() as log_collector:
        check_transports(etree.ElementTree(etree.XML(raw_xml)))

    return log_collector.get_collected(clean=True).strip().splitlines(False)


@pytest.mark.parametrize('xml', ['''
<channel t_type="bus">
<group>
    <threads>
        <thread/>
    </threads>
</group>
</channel>
''', '''
<channel t_type="bus" subtype="bus">
<group>
    <threads>
        <thread t_type="water" subtype="river"/>
    </threads>
</group>
</channel>
'''])
@pytest.mark.dbuser
def test_valid_transports(xml):
    messages = check_transports_messages(xml)
    assert not any(message.startswith('WARNING') for message in messages)


@pytest.mark.parametrize('xml,warning', [(
    '''
    <channel t_type="bicycle">
    <group>
        <threads>
            <thread/>
        </threads>
    </group>
    </channel>
    ''',
    u'Строка 2. Неизвестный тип транспорта "bicycle".'
), (
    '''
    <channel>
    <group>
        <threads>
            <thread t_type="bicycle"/>
        </threads>
    </group>
    </channel>
    ''',
    u'Строка 5. Неизвестный тип транспорта "bicycle".'
), (
    '''
    <channel subtype="scooter">
    <group>
        <threads>
            <thread/>
        </threads>
    </group>
    </channel>
    ''',
    u'Строка 2. Неизвестный подтип транспорта "scooter".'
), (
    '''
    <channel>
    <group>
        <threads>
            <thread subtype="scooter"/>
        </threads>
    </group>
    </channel>
    ''',
    u'Строка 5. Неизвестный подтип транспорта "scooter".'
), (
    '''
    <channel t_type="water" subtype="train">
    <group>
        <threads>
            <thread/>
        </threads>
    </group>
    </channel>
    ''',
    u'Строка 2. Подтип транспорта "train" не относится к типу "water".'
), (
    '''
    <channel>
    <group>
        <threads>
            <thread t_type="water" subtype="train"/>
        </threads>
    </group>
    </channel>
    ''',
    u'Строка 5. Подтип транспорта "train" не относится к типу "water".'
)])
@pytest.mark.dbuser
def test_invalid_transports(xml, warning):
    messages = check_transports_messages(xml)

    assert len(messages) == 1
    assert messages[0] == u'WARNING: {}'.format(warning)
