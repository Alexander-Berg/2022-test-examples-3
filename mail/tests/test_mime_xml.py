import pytest
from copy import deepcopy
from ora2pg import mime_xml as MX
from pymdb.types import MailMimePart

MANDATORY_MIME_PART_ARGS = dict(
    id='1',
    offset='0',
    length='10',
)


def test_str_to_positive_int_for_noninteger():
    with pytest.raises(MX.ConvertError):
        MX.str_to_positive_int('field', '123a')


def test_str_to_positive_int_for_negative_integer():
    with pytest.raises(MX.ConvertError):
        MX.str_to_positive_int('field', '-123')


@pytest.mark.parametrize('missed_arg', ['id', 'offset', 'length'])
def test_create_mime_part_without_mandatory_argument(missed_arg):
    kwargs = deepcopy(MANDATORY_MIME_PART_ARGS)
    del kwargs[missed_arg]
    with pytest.raises(MX.MissedArgumentError):
        MX.MimePartConvertor(**kwargs)


@pytest.mark.parametrize(('arg_name', 'attr_name'), [
    ('id', 'hid'),
    ('content_type.type', 'content_type'),
    ('content_type.subtype', 'content_subtype'),
    ('content_type.name', 'name'),
    ('content_type.charset', 'charset'),
    ('content_transfer_encoding', 'encoding'),
    ('content_disposition.value', 'content_disposition'),
    ('content_disposition.filename', 'filename'),
    ('content_id', 'cid'),
])
def test_create_mime_part_inits_correct_attribute(arg_name, attr_name):
    kwargs = deepcopy(MANDATORY_MIME_PART_ARGS)
    kwargs[arg_name] = 'test'
    mime_part = MX.MimePartConvertor(**kwargs)
    assert getattr(mime_part, attr_name) == 'test'


def test_create_mime_part_sets_correct_offsets():
    mime_part = MX.MimePartConvertor(id='1', offset='10', length='100')
    assert mime_part.offset_begin == 10
    assert mime_part.offset_end == 110


def test_parse_mime_xml_for_malfomed_xml():
    with pytest.raises(MX.MimeXmlParseError):
        MX.parse_mime_xml('<?xml version="1.0"?>\n<message><part id="1"></message>')


def test_parse_mime_xml_for_xml_without_parts():
    with pytest.raises(MX.MimeXmlNoPartsError):
        MX.parse_mime_xml('<?xml version="1.0"?>\n<message></message>')


def test_parse_mime_xml_for_None():
    assert MX.parse_mime_xml(None) is None


def test_parse_mime_xml_for_Empty():
    assert MX.parse_mime_xml('') is None


XML_MIME_WITH_NESTED_PARTS = '''<?xml version="1.0" encoding="windows-1251"?>
<message>
<part id="1" offset="1812" length="56419"
>
    <part id="1.1" offset="1986" length="4114"
    >
        <part id="1.1.1" offset="2122" length="471"
        >
        </part>

        <part id="1.1.2" offset="2728" length="3329"
        >
        </part>

    </part>

    <part id="1.2" offset="6314" length="51874"
    >
    </part>
</part>
</message>
'''


def test_parse_mime_xml_for_nested_parts_returns_all_parts():
    mime = MX.parse_mime_xml(XML_MIME_WITH_NESTED_PARTS)
    assert len(mime) == 5


XML_MIME_WITH_GOOD_XML = '''<?xml version="1.0" encoding="windows-1251"?>
<message>
    <part id="1" offset="10" length="100"
        content_type.type="application"
        content_type.subtype="vnd.ms-excel"
        content_type.charset="US-ASCII"
        content_type.name="my"
        content_transfer_encoding="base64"
        content_disposition.value="attachment"
        content_disposition.filename="my.xls"
    >
    </part>
</message>
'''

MIME_PARTS = [MailMimePart(
    hid='1',
    content_type='application',
    content_subtype='vnd.ms-excel',
    boundary='',
    name='my',
    charset='US-ASCII',
    encoding='base64',
    content_disposition='attachment',
    filename='my.xls',
    cid='',
    offset_begin=10,
    offset_end=110
)]


def test_parse_mime_xml_for_good_xml():
    mime = MX.parse_mime_xml(XML_MIME_WITH_GOOD_XML)
    assert mime == MIME_PARTS
