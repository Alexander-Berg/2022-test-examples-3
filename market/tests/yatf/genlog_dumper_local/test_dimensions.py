# coding: utf-8
import pytest

from hamcrest import assert_that

from market.idx.generation.yatf.resources.genlog_dumper.input_records_proto import InputRecordsProto, make_gl_record
from market.idx.generation.yatf.resources.genlog_dumper.input_run_options import RunOptions
from market.idx.generation.yatf.test_envs.genlog_dumper import (
    GenlogDumperTestEnv,
    RUN_RESOURCE_NAME,
    OFFERS_RESOURCE_NAME
)

from market.idx.generation.yatf.matchers.genlog_dumper.env_matchers import HasDimensionsRecord
from market.idx.pylibrary.offer_flags.flags import OfferFlags


def get_binary_ware_md5(id):
    return (id + '==').decode('base64')


def get_ware_md5(offerid):
    # допускаем, что offerid односимвольный
    return offerid + 'irstOffer0V7gLLUBANyg'


@pytest.fixture(scope='module')
def offers():
    return [
        make_gl_record(
            category_id=91491,
            offer_id='1',
            is_blue_offer=True,
            ware_md5=get_ware_md5('1'),
            binary_ware_md5=get_binary_ware_md5(get_ware_md5('1'),),
            flags=OfferFlags.IS_FULFILLMENT,
            cpa=4,
            weight=1,
            height=2,
            width=3,
            length=4,
        ),
        make_gl_record(
            category_id=91491,
            offer_id='2',
            ware_md5=get_ware_md5('2'),
            binary_ware_md5=get_binary_ware_md5(get_ware_md5('2'),),
            is_blue_offer=False,
            cpa=3,
            weight=5,
            height=6,
            width=7,
            length=8,
        ),
        make_gl_record(
            category_id=91491,
            offer_id='3',
            is_blue_offer=True,
            ware_md5=get_ware_md5('3'),
            binary_ware_md5=get_binary_ware_md5(get_ware_md5('3'),),
            flags=OfferFlags.IS_FULFILLMENT,
            cpa=4,
            weight=7,
            height=8,
            width=9,
            length=10,
        ),
        make_gl_record(
            category_id=91491,
            offer_id='4',
            ware_md5=get_ware_md5('4'),
            binary_ware_md5=get_binary_ware_md5(get_ware_md5('4'),),
            is_blue_offer=True,
            flags=OfferFlags.IS_FULFILLMENT,
            cpa=4,
            weight=7,
            height=8,
            width=9,
            length=10,
        ),
        make_gl_record(
            category_id=91491,
            offer_id='5',
            ware_md5=get_ware_md5('5'),
            binary_ware_md5=get_binary_ware_md5(get_ware_md5('5'),),
            is_blue_offer=False,
            cpa=4,
            weight=17.1,
            height=18.2,
            width=19,
            length=20,
        ),
    ]


@pytest.yield_fixture(scope="module")
def genlog_dumper(offers):
    gd_resources = {
        RUN_RESOURCE_NAME: RunOptions([
            '--dumper', 'DIMENSIONS',
            '--dumper', 'WARE_MD5',
        ]),
        OFFERS_RESOURCE_NAME: InputRecordsProto(offers)
    }
    with GenlogDumperTestEnv(**gd_resources) as env:
        env.execute()
        env.verify()
        yield env


def test_dimenisions_mmap(genlog_dumper, offers):
    '''
        Проверяем качественый состав blue_offer_dimensions.mmap
        Попасть в него должны только cpa оферы
    '''

    expected = [('Version', '2')]
    ordered_offers = genlog_dumper.ordered_offers(offers)

    for offset, offer in enumerate(ordered_offers):
        if offer.HasField('cpa') and getattr(offer, 'cpa') == 4:
            el = [
                ('OfferOffset', str(offset)),
                ('Weight', '{0:g}'.format(offer.weight)),
                ('Length', '{0:g}'.format(offer.length)),
                ('Width', '{0:g}'.format(offer.width)),
                ('Height', '{0:g}'.format(offer.height)),
            ]
            expected.extend(el)

    assert_that(
        genlog_dumper,
        HasDimensionsRecord(expected),
        u'blue_offer_dimensions.mmap contains expected document'
    )
