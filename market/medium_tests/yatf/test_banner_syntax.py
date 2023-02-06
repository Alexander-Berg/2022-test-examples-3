# coding: utf-8
import pytest

from market.idx.yatf.resources.yql_resource import YtResource, YqlRequestResource
from market.idx.yatf.resources.yt_stuff_resource import (
    get_yt_prefix
)

from market.idx.yatf.test_envs.yql_env import YqlRunnerTestEnv
import market.idx.pylibrary.mindexer_core.banner.banner as banner

from yt.wrapper import ypath_join

from hamcrest import (
    assert_that,
    contains_string,
    is_not
)


GENERATION = '20191010_1010'
NAME = 'model_feed'
FEED_NAME = 'feed_name'
REGION = 213


class StubConfig(object):
    """Stub config class, only needed because object() doesn't have __dict__.
    """
    def output_yt_path(self, nodepath):
        return ypath_join(get_yt_prefix(), 'out', nodepath)

    def resolve(self, tmpl, **kwargs):
        subst = dict()
        subst.update(self.__dict__)
        subst.update(kwargs)
        return tmpl.format(**subst)


class YqlDependency(object):
    def __init__(self, name, result_table):
        self.name = name
        self.result_table = result_table


@pytest.fixture(scope='module')
def config(yt_server):
    config = StubConfig()

    config.yt_proxy = yt_server.get_server()
    config.yt_home_dir = get_yt_prefix()
    config.yt_pool_batch = 'market-production-batch'
    config.banner_adult_root_category = list()
    config.banner_bad_parent_categories = '1,2,3'
    config.banner_bad_vendors = '242456'
    config.banner_good_content_enabled = False
    config.banner_beru_shop_id = 12345

    return config


@pytest.fixture(scope='module')
def syntax_verify_workflow(
    yt_server,
):
    resources = {
        'yt': YtResource(yt_stuff=yt_server),
    }

    with YqlRunnerTestEnv(syntax_version=1, **resources) as test_env:
        yield test_env


def create_yql_request(sql):
    request = sql._yql_statement
    request = '\n'.join(request.split('\n')[1:])

    return YqlRequestResource(request)


def test_vendor_feed(syntax_verify_workflow, config):
    sql = banner.VendorFeedProcessor(
        'vendor',
        config,
        GENERATION,
        FEED_NAME,
        REGION,
        yql_dependency=[YqlDependency(NAME, '//home/out/model_feed'), ]
    )

    request = sql._yql_statement
    request = '\n'.join(request.split('\n')[1:])
    req = YqlRequestResource(request)
    result = syntax_verify_workflow.validate(req)

    assert_that(result.special, is_not(contains_string('Error')), result)


def test_model_vendor_simple(syntax_verify_workflow, config):
    sql = banner.ModelVendorSimpleYqlProcessor(config, GENERATION)

    result = syntax_verify_workflow.validate(create_yql_request(sql))

    assert_that(result.special, is_not(contains_string('Error')), result)


def test_model_vendor(syntax_verify_workflow, config):
    sql = banner.ModelVendorYqlProcessor(config, GENERATION)

    result = syntax_verify_workflow.validate(create_yql_request(sql))

    assert_that(result.special, is_not(contains_string('Error')), result)


def test_model_feed(syntax_verify_workflow, config):
    sql = banner.ModelFeedProcessor(NAME, config, GENERATION, FEED_NAME, 2, REGION)

    result = syntax_verify_workflow.validate(create_yql_request(sql))

    assert_that(result.special, is_not(contains_string('Error')), result)


def test_blue_offers_feed(syntax_verify_workflow, config):
    sql = banner.BlueOffersFeedProcessor(NAME, config, GENERATION, FEED_NAME)

    result = syntax_verify_workflow.validate(create_yql_request(sql))

    assert_that(result.special, is_not(contains_string('Error')), result)


def test_blue_vendor_category_feed(syntax_verify_workflow, config):
    sql = banner.BlueVendorCategoryFeedProcessor(NAME, config, GENERATION, FEED_NAME)

    result = syntax_verify_workflow.validate(create_yql_request(sql))

    assert_that(result.special, is_not(contains_string('Error')), result)


@pytest.skip('TODO: skip reason')
def test_google_reviews_feed(syntax_verify_workflow, config):
    sql = banner.GoogleReviewsFeedProcessor(NAME, config, GENERATION, FEED_NAME)

    result = syntax_verify_workflow.validate(create_yql_request(sql))

    assert_that(result.special, is_not(contains_string('Error')), result)


def test_google_dsa_offers_feed(syntax_verify_workflow, config):
    sql = banner.GoogleDsaOffersFeedProcessor(NAME, config, GENERATION, FEED_NAME)

    result = syntax_verify_workflow.validate(create_yql_request(sql))

    assert_that(result.special, is_not(contains_string('Error')), result)


def test_beru_offers_feed(syntax_verify_workflow, config):
    sql = banner.BeruOffersFeedProcessor(NAME, config, GENERATION, FEED_NAME)

    result = syntax_verify_workflow.validate(create_yql_request(sql))

    assert_that(result.special, is_not(contains_string('Error')), result)


def test_google_adaptiev_models_feed(syntax_verify_workflow, config):
    sql = banner.GoogleAdaptiveModelsFeedProcessor(NAME, config, GENERATION, FEED_NAME, REGION, 2)

    result = syntax_verify_workflow.validate(create_yql_request(sql))

    assert_that(result.special, is_not(contains_string('Error')), result)


def test_cluster_feed(syntax_verify_workflow, config):
    sql = banner.ClusterFeedProcessor(NAME, config, GENERATION, FEED_NAME, REGION, 2)

    result = syntax_verify_workflow.validate(create_yql_request(sql))

    assert_that(result.special, is_not(contains_string('Error')), result)


def test_blue_missing_brands_feed(syntax_verify_workflow, config):
    sql = banner.BlueMissingBrandsFeedProcessor(NAME, config, GENERATION, FEED_NAME)

    result = syntax_verify_workflow.validate(create_yql_request(sql))

    assert_that(result.special, is_not(contains_string('Error')), result)


def test_blue_adult_offers_feed(syntax_verify_workflow, config):
    sql = banner.BlueAdultOffersFeedProcessor(NAME, config, GENERATION, FEED_NAME)

    result = syntax_verify_workflow.validate(create_yql_request(sql))

    assert_that(result.special, is_not(contains_string('Error')), result)


@pytest.skip('TODO: skip reason')
def test_beru_google_merchant_feed(syntax_verify_workflow, config):
    sql = banner.BeruGoogleMerchantXmlOffersFeedProcessor(NAME, config, GENERATION, FEED_NAME, [1, ], config.banner_bad_parent_categories, False)

    result = syntax_verify_workflow.validate(create_yql_request(sql))

    assert_that(result.special, is_not(contains_string('Error')), create_yql_request(sql))
