# -*- coding: utf-8 -*-

from enum import Enum
from typing import Dict, List, Tuple, Optional
import dataclasses

from travel.hotels.content_manager.data_model.types import uint
from travel.hotels.content_manager.lib.common import dc_from_dict, get_dc_yt_schema, get_dc_yql_schema, hide_secrets_dc


class E(Enum):
    EV_0 = 'ev_0'
    EV_1 = 'ev_1'


@dataclasses.dataclass
class A:
    fi: int
    fs: str = 'no way'
    secret: str = 'what?'


@dataclasses.dataclass
class B:
    a1: Optional[List[A]]
    a2: Dict[str, A]
    a3: Tuple[A]


@dataclasses.dataclass
class C:
    fs: str
    fi: int
    fu: uint
    fb: bool
    fd: Dict[str, str]
    fl: List[int]
    fe: E


@dataclasses.dataclass
class D:
    fi: int = dataclasses.field(metadata={'converter': lambda x: int(x)})


def test_dc_from_dict():
    d = {
        'a1': [
            {
                'fi': 5,
                'fs': 'hi',
            },
        ],
        'a2': {
            'key': {
                'fi': 7,
                'fs': 'bye',
            }
        },
        'a3': (
            {
                'fi': 99,
            },
        ),
    }

    dc: B = dc_from_dict(B, d)

    assert 5 == dc.a1[0].fi
    assert 'hi' == dc.a1[0].fs

    assert 7 == dc.a2['key'].fi
    assert 'bye' == dc.a2['key'].fs

    assert 99 == dc.a3[0].fi
    assert 'no way' == dc.a3[0].fs

    d = {
        'fs': 'fs',
        'fi': 1,
        'fu': 2,
        'fb': True,
        'fd': {},
        'fl': [],
        'fe': 'ev_1',
    }

    dc: C = dc_from_dict(C, d)

    assert E.EV_1 == dc.fe

    d = {
        'fi': '777',
    }

    dc: D = dc_from_dict(D, d)

    assert 777 == dc.fi


def test_get_dc_yt_schema():
    schema = get_dc_yt_schema(C)
    exp = {
        'fs': 'string',
        'fi': 'int64',
        'fu': 'uint64',
        'fb': 'boolean',
        'fd': 'any',
        'fl': 'any',
        'fe': 'string',
    }

    assert exp == schema


def test_get_dc_yql_schema():
    schema = get_dc_yql_schema(C)
    exp = {
        'fs': 'String',
        'fi': 'Int64',
        'fu': 'UInt64',
        'fb': 'Bool',
        'fd': 'Any',
        'fl': 'Any',
        'fe': 'String',
    }

    assert exp == schema


def test_hide_secrets_dc():
    b = B(
        a1=[
            A(
                fi=123,
                fs='AQAD-AQAD-AQAD-AQAD-AQAD',
            ),
        ],
        a2={
            'key': A(
                fi=123,
            ),
        },
        a3=(
            A(
                fi=123,
                fs='AQAD-AQAD-AQAD-AQAD-AQAD',
            ),
        ),
    )
    b = hide_secrets_dc(b)

    assert '******AQAD' == b.a1[0].fs
    assert '******hat?' == b.a1[0].secret
    assert 'no way' == b.a2['key'].fs
    assert '******hat?' == b.a2['key'].secret
    assert '******AQAD' == b.a3[0].fs
    assert '******hat?' == b.a3[0].secret
