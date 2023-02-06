# coding: utf-8

import pytest

from hamcrest import assert_that

from market.idx.generation.yatf.matchers.genlog_dumper.env_matchers import HasBaseOfferPropsFbRecursive
from market.idx.generation.yatf.resources.genlog_dumper.input_records_proto import InputRecordsProto, make_gl_record
from market.idx.generation.yatf.resources.genlog_dumper.input_run_options import RunOptions
from market.idx.generation.yatf.test_envs.genlog_dumper import (
    GenlogDumperTestEnv,
    RUN_RESOURCE_NAME,
    OFFERS_RESOURCE_NAME
)
from market.idx.offers.yatf.utils.fixtures import default_credit_template


def get_binary_ware_md5(id):
    return (id + '==').decode('base64')


@pytest.fixture(scope='module')
def offers():
    return [
        make_gl_record(
            offer_id='no_credit',
            ware_md5='1jk5ZDA1MmNlYzhjNDI5YQ',
            binary_ware_md5=get_binary_ware_md5('1jk5ZDA1MmNlYzhjNDI5YQ'),
            credit_templates=[
            ],
        ),
        make_gl_record(
            offer_id='has_credit',
            ware_md5='2WQwZDFkMzNjMjc1NGMwOQ',
            binary_ware_md5=get_binary_ware_md5('2WQwZDFkMzNjMjc1NGMwOQ'),
            credit_templates=[
                default_credit_template(),
            ],
        ),
        make_gl_record(
            offer_id='has_installment',
            ware_md5='3DY0Y2VkMWI3MzAzNDJjZQ',
            binary_ware_md5=get_binary_ware_md5('3DY0Y2VkMWI3MzAzNDJjZQ'),
            credit_templates=[
                default_credit_template(is_installment=True),
            ],
        ),
    ]


@pytest.yield_fixture(scope="module")
def genlog_dumper(offers):
    gd_resources = {
        RUN_RESOURCE_NAME: RunOptions([
            '--dumper', 'BASE_OFFER_PROPS',
            '--dumper', 'WARE_MD5',
        ]),
        OFFERS_RESOURCE_NAME: InputRecordsProto(offers)
    }
    with GenlogDumperTestEnv(**gd_resources) as env:
        env.execute()
        env.verify()
        yield env


def test_store_id_in_base_offer_props(genlog_dumper, offers):
    expected = {}

    ordered_offers = genlog_dumper.ordered_offers(offers)

    for offset, offer in enumerate(ordered_offers):
        if len(offer.credit_templates) > 0:
            expected[offset] = {'CreditTemplateIds': [1]}
        else:
            expected[offset] = {'CreditTemplateIds': None}

    assert_that(
        genlog_dumper,
        HasBaseOfferPropsFbRecursive(expected)
    )
