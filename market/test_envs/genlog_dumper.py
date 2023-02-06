# coding: utf-8

import os
import yatest.common
from market.idx.yatf.resources.yt_token_resource import YtTokenResource

from market.proto.indexer.GenerationLog_pb2 import Record
from market.pylibrary.memoize.memoize import memoize

from market.idx.yatf.common import ignore_errors
from market.idx.yatf.matchers.env_matchers import HasExitCode
from market.idx.yatf.resources.resource import FileResource
from market.idx.yatf.test_envs.base_env import BaseEnv

from market.idx.generation.yatf.resources.genlog_dumper.input_records_proto import (
    InputRecordsProto,
    DEFAULT_PROTO_DIR
)
from market.idx.generation.yatf.resources.genlog_dumper.input_run_options import RunOptions
from market.idx.generation.yatf.resources.genlog_dumper.output_content_offer import ContentOfferTsv
from market.idx.offers.yatf.resources.offers_indexer.base_offer_props import BaseOfferProps
from market.idx.offers.yatf.resources.offers_indexer.blue_offer_models import BlueOfferModels
from market.idx.offers.yatf.resources.offers_indexer.offers_delivery_info import OffersDeliveryInfo
from market.idx.offers.yatf.resources.offers_indexer.offers_hash_mapping import OffersHashMapping
from market.idx.offers.yatf.resources.offers_indexer.gl_sc import GlSc
from market.idx.offers.yatf.resources.offers_indexer.offer_delivery_buckets_reader import OfferDeliveryBucketsReader
from market.idx.offers.yatf.resources.offers_indexer.ware_md5_values_binary import WareMd5Values
from market.idx.offers.yatf.resources.offers_indexer.offer_dimensions import OfferDimensions
from market.idx.offers.yatf.resources.offers_indexer.offer_sku import OfferSku
from market.idx.offers.yatf.resources.offers_indexer.sku_position import SkuPosition


def _IDX_STUBS_DIR():
    return os.path.join(
        yatest.common.source_path(),
        'market', 'idx', 'offers', 'yatf',
        'resources',
        'offers_indexer',
        'stubs'
    )


OFFERS_TABLE_RESOURCE_NAME = 'offers_table'
OFFERS_RESOURCE_NAME = 'offers'
RUN_RESOURCE_NAME = 'run_options'


class GenlogDumperTestEnv(BaseEnv):
    _MATCHERS = [
        HasExitCode(0)
    ]

    def __init__(self,  yt_stuff=None, **resources):
        self._STUBS = {
            name: FileResource(os.path.join(_IDX_STUBS_DIR(), filename))
            for name, filename in {
                'currency_rates_xml': 'currency_rates.xml',
                'shops_dat': 'shops-utf8.dat.report.generated',
                'delivery_holidays_xml': 'delivery_holidays.xml',
            }.items()
        }
        super(GenlogDumperTestEnv, self).__init__(**resources)
        resources_stubs = {
            OFFERS_RESOURCE_NAME: InputRecordsProto([Record()]),
            RUN_RESOURCE_NAME: RunOptions([]),
            'yt_token': YtTokenResource(),
        }

        self.yt_stuff = yt_stuff
        if yt_stuff:
            self.yt_client = yt_stuff.get_yt_client()
        else:
            self.yt_client = None

        ignore_errors(os.makedirs, OSError)(self.proto_input_dir)

        for name, val in resources_stubs.items():
            if name not in self.resources:
                self.resources[name] = val

    @property
    def index_dir(self):
        return self.output_dir   # for env_matchers

    @property
    def proto_input_dir(self):
        return os.path.join(self.input_dir, DEFAULT_PROTO_DIR)

    @property
    def description(self):
        return 'genlog_dumper'

    @property
    def executable_path(self):
        return os.path.join('market', 'idx', 'generation', 'genlog_dumper', 'genlog_dumper')

    @property
    @memoize()
    def base_offer_props(self):
        result = self.outputs['base_offer_props_fb']
        return result.load()

    @property
    @memoize()
    def base_offer_props_ext(self):
        result = self.outputs['base_offer_props_ext_fb64']
        return result.load()

    @property
    @memoize()
    def blue_offer_models(self):
        result = self.outputs['blue_offer_models_txt']
        return result.load()

    @property
    @memoize()
    def content_offers(self):
        result = self.outputs['content_offers']
        return result.load()

    @property
    @memoize()
    def gl_sc(self):
        result = self.outputs['gl_sc_mmap']
        return result.load()

    @property
    @memoize()
    def offer_delivery_buckets_mmap(self):
        result = self.outputs['offer_delivery_buckets_mmap']
        return result.load()

    @property
    @memoize()
    def offers_delivery_info(self):
        result = self.outputs['offers_delivery_info_fb']
        return result.load()

    @property
    @memoize()
    def offer_dimensions(self):
        result = self.outputs['blue_offer_dimensions_mmap']
        return result.load()

    @property
    @memoize()
    def offers_hash_mapping(self):
        result = self.outputs['offers_hash_mapping_fb']
        return result.load()

    @property
    @memoize()
    def offer_sku(self):
        result = self.outputs['offer_sku_mmap']
        return result.load()

    @property
    @memoize()
    def sku_position(self):
        result = self.outputs['sku_position_mmap']
        return result.load()

    @property
    @memoize()
    def ware_md5(self):
        result = self.outputs['ware_md5_values_binary']
        result.load()
        return result

    def ordered_offers(self, offers):
        # make sure you include WARE_MD5 dumper to your workflow
        offset_by_md5 = {}

        for i, _ in enumerate(offers):
            offset_by_md5[self.ware_md5.get_ware_md5(i)] = i

        return sorted(offers, key=lambda offer : offset_by_md5[offer.ware_md5])

    def execute(
            self,
            enable_hard2_dssm=False,
            enable_reformulation_dssm=False,
            enable_bert=False,
            enable_super_embed=False,
            path=None,
            gl_sc_mmap_version=2,
            clear_old_bucket_ids_for_white_offers=False,
            clear_buckets_for_wcpa=False,
            enable_assessment_binary=False,
            enable_assessment=False,
            enable_click=False,
            enable_has_cpa_click=False,
            enable_cpa=False,
            enable_billed_cpa=False
    ):
        if path is None:
            relative_path = self.executable_path
            absolute_path = yatest.common.binary_path(relative_path)
            path = absolute_path

        cmd = [path]
        if self.yt_stuff:
            cmd.extend(['--input',  self.resources[OFFERS_TABLE_RESOURCE_NAME].get_path()])
            cmd.extend(['--yt-proxy', self.yt_stuff.get_server()])
            cmd.extend(['--yt-token-path', self.resources['yt_token'].path])
        else:
            cmd.append('--local-mode')
            cmd.extend(['--input', self.proto_input_dir])
        cmd.extend(['--output', self.output_dir])
        cmd.extend(['--currency-rates-path', self.resources['currency_rates_xml'].path]),
        cmd.extend(['--shops-dat-path', self.resources['shops_dat'].path]),
        cmd.extend(['--delivery-holidays-path', self.resources['delivery_holidays_xml'].path]),
        cmd.extend(['--input-ctr-path', _IDX_STUBS_DIR()]),
        cmd.extend(self.resources[RUN_RESOURCE_NAME].options_list)

        if enable_hard2_dssm:
            cmd.append('--enable-hard2-dssm')
        if enable_reformulation_dssm:
            cmd.append('--enable-reformulation-dssm')
        if enable_bert:
            cmd.append('--enable-bert-dssm')
        if enable_super_embed:
            cmd.append('--enable-super-embed')
        if enable_assessment_binary:
            cmd.append('--categories')
            cmd.append('--enable-assessment-binary')
        if enable_assessment:
            cmd.append('--categories')
            cmd.append('--enable-assessment')
        if enable_click:
            cmd.append('--categories')
            cmd.append('--enable-click')
        if enable_has_cpa_click:
            cmd.append('--categories')
            cmd.append('--enable-has-cpa-click')
        if enable_cpa:
            cmd.append('--categories')
            cmd.append('--enable-cpa')
        if enable_billed_cpa:
            cmd.append('--categories')
            cmd.append('--enable-billed-cpa')
        if clear_old_bucket_ids_for_white_offers:
            cmd.append('--clear-old-bucket-ids-from-white-offers')
        if clear_buckets_for_wcpa:
            cmd.append('--clear-buckets-for-wcpa')
        cmd.extend(['--gl-sc-mmap-version', str(gl_sc_mmap_version)]),
        self.exec_result = self.try_execute_under_gdb(
            cmd,
            cwd=self.output_dir,
            check_exit_code=False
        )

        self.outputs.update({
            'content_offers': ContentOfferTsv(os.path.join(self.output_dir, 'content-offer.tsv')),
            'base_offer_props_fb': BaseOfferProps(os.path.join(self.output_dir, 'base-offer-props.fb')),
            'base_offer_props_ext_fb64': BaseOfferProps(os.path.join(self.output_dir, 'base-offer-props-ext.fb64')),
            'base_docs_props_meta': FileResource(os.path.join(self.output_dir, 'base_docs_props.meta')),
            'offers_delivery_info_fb': OffersDeliveryInfo(os.path.join(self.output_dir, 'offers-delivery-info.fb')),
            'dssm_values_binary': FileResource(os.path.join(self.output_dir, 'dssm.values.binary')),
            'hard2_dssm_values_binary': FileResource(os.path.join(self.output_dir, 'hard2_dssm.values.binary')),
            'reformulation_dssm_values_binary': FileResource(os.path.join(self.output_dir, 'reformulation_dssm.values.binary')),
            'bert_dssm_values_binary': FileResource(os.path.join(self.output_dir, 'bert_dssm.values.binary')),
            'super_embed_values_binary': FileResource(os.path.join(self.output_dir, 'super_embed.values.binary')),
            'assessment_binary_values_binary': FileResource(os.path.join(self.output_dir, 'assessment_binary.values.binary')),
            'assessment_values_binary': FileResource(os.path.join(self.output_dir, 'assessment.values.binary')),
            'click_values_binary': FileResource(os.path.join(self.output_dir, 'click.values.binary')),
            'has_cpa_click_values_binary': FileResource(os.path.join(self.output_dir, 'has_cpa_click.values.binary')),
            'cpa_values_binary': FileResource(os.path.join(self.output_dir, 'cpa.values.binary')),
            'billed_cpa_values_binary': FileResource(os.path.join(self.output_dir, 'billed_cpa.values.binary')),
            'gl_sc_mmap': GlSc(os.path.join(self.output_dir, 'gl_sc.mmap')),
            'bids_meta_binary': BaseOfferProps(os.path.join(self.output_dir, 'bids.meta.binary')),
            'minimal_bids_values_binary_v2': OffersDeliveryInfo(os.path.join(self.output_dir, 'minimal.bids.values.binary.v2')),
            'vendor_values_binary': FileResource(os.path.join(self.output_dir, 'vendor.values.binary')),
            'ware_md5_values_binary': WareMd5Values(os.path.join(self.output_dir, 'ware_md5.values.binary')),
            'content_offer_tsv': FileResource(os.path.join(self.output_dir, 'content-offer.tsv')),
            'vat_props_values_binary': FileResource(os.path.join(self.output_dir, 'vat_props.values.binary')),
            'offer_promo_mmap': FileResource(os.path.join(self.output_dir, 'offer_promo.mmap')),
            'feedid_offerid_sequence_binary': FileResource(os.path.join(self.output_dir, 'feedid_offerid.sequence.binary')),
            'offer_delivery_buckets_mmap': OfferDeliveryBucketsReader(os.path.join(self.output_dir, 'offer_delivery_buckets.mmap')),
            'local_delivery_yml_mmap': FileResource(os.path.join(self.output_dir, 'local_delivery_yml.mmap')),
            'bids_timestamps_fb': FileResource(os.path.join(self.output_dir, 'bids-timestamps.fb')),
            'offers_hash_mapping_fb': OffersHashMapping(os.path.join(self.output_dir, 'offers-hash-mapping.fb')),
            'blue_offer_dimensions_mmap': OfferDimensions(os.path.join(self.index_dir, 'blue_offer_dimensions.mmap')),
            'offer_sku_mmap': OfferSku(os.path.join(self.index_dir, 'offer_sku.mmap')),
            'sku_position_mmap': SkuPosition(os.path.join(self.index_dir, 'msku_positions.mmap')),
            'blue_offer_models_txt': BlueOfferModels(os.path.join(self.index_dir, 'blue_offer_models.txt')),
        })
