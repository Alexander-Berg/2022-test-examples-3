
from textwrap import dedent
import re

from search.tools.diff_two_serps.lib.util import parse_report, apply_toplevel_mask, normalize_response_body, EAction


def test_parse_report_basic():
    text = dedent("""\
        Head {
          Version: 1
          SegmentId: "vla1-0592-vla-multibeta7-mmeta-21716.gencfg-c.yandex.net"
        }
      """).encode('utf-8')
    res = parse_report(text)
    assert res.Head.Version == 1
    assert res.Head.SegmentId == b'vla1-0592-vla-multibeta7-mmeta-21716.gencfg-c.yandex.net'


def test_apply_toplevel_mask():
    text = dedent("""\
        Head {
          Version: 1
          SegmentId: "vla1-0592-vla-multibeta7-mmeta-21716.gencfg-c.yandex.net"
        }
        SearcherProp {
          Key: "Some key"
          Value: "123"
        }
        SearcherProp {
          Key: "Some another key"
          Value: "228"
        }
        SearcherProp {
          Key: "More key"
          Value: "1448"
        }
        TotalDocCount: 8958
      """).encode('utf-8')
    mask = {
        "Head": EAction.KeepField,
        "DebugInfo": EAction.ClearField,
        "SearcherProp": {
            "Key": b'Some key',
        },
    }
    res = apply_toplevel_mask(text, False, mask)
    assert res == dedent("""\
      Head {
        Version: 1
        SegmentId: "vla1-0592-vla-multibeta7-mmeta-21716.gencfg-c.yandex.net"
      }
      SearcherProp {
        Key: "Some key"
        Value: "123"
      }
      """)


def test_normalize_response_body():
    text = dedent("""\
        Head {
          Version: 1
          SegmentId: "vla1-0592-vla-multibeta7-mmeta-21716.gencfg-c.yandex.net"
        }
        SearcherProp {
          Key: "Some key"
          Value: "123"
        }
        SearcherProp {
          Key: "Some another key"
          Value: "228"
        }
        SearcherProp {
          Key: "More key"
          Value: "1448"
        }
        TotalDocCount: 8958
      """).encode('utf-8')
    mask = {
        "Head": EAction.KeepField,
        "DebugInfo": EAction.ClearField,
        "SearcherProp": {
            "Key": b'Some key',
        },
        "TotalDocCount": EAction.KeepField
    }
    res = normalize_response_body(text, dict_mask=mask)
    assert res == dedent("""\
      Head {
        Version: 1
        SegmentId: "vla1-0592-vla-multibeta7-mmeta-21716.gencfg-c.yandex.net"
      }
      SearcherProp {
        Key: "Some key"
        Value: "123"
      }
      TotalDocCount: 8958
      """)
    res = normalize_response_body(text, dict_mask=mask, remove_line_regex=[re.compile('TotalDocCount:\\s*\\d+')])
    assert res == dedent("""\
      Head {
        Version: 1
        SegmentId: "vla1-0592-vla-multibeta7-mmeta-21716.gencfg-c.yandex.net"
      }
      SearcherProp {
        Key: "Some key"
        Value: "123"
      }
      """)
