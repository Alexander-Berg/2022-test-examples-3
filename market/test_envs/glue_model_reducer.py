# coding: utf-8

import os
import yatest.common

from google.protobuf import text_format

from market.idx.yatf.matchers.env_matchers import HasExitCode
from market.idx.yatf.test_envs.base_env import BaseEnv

from market.idx.offers.bin.glue_model_reducer.proto.options_pb2 import TGlueModelReducerOptions
from market.idx.offers.bin.msku_keys_extractor.proto.options_pb2 import TMskuKeysExtractorOptions
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix

from yt.wrapper.ypath import ypath_join


class GlueModelReducerTestEnv(BaseEnv):
    _MATCHERS = [
        HasExitCode(0)
    ]

    def __init__(
            self,
            yt_stuff,
            output_yt_path,
            input_offers_yt_paths=None,
            input_model_glue_yt_paths=None,
            **resources
    ):
        super(GlueModelReducerTestEnv, self).__init__(**resources)

        self.yt_stuff = yt_stuff
        self.yt_client = yt_stuff.get_yt_client()

        self.yt_output_table = output_yt_path
        self.yt_input_offers_path = input_offers_yt_paths or list()
        self.input_model_glue_yt_paths = input_model_glue_yt_paths or list()

    @property
    def description(self):
        return 'glue_msku_reducer'

    def execute(self, binary_path=None):
        if binary_path is None:
            relative_path = os.path.join('market', 'idx', 'offers', 'bin', 'glue_model_reducer', 'bin', 'glue_model_reducer')
            absolute_path = yatest.common.binary_path(relative_path)
            binary_path = absolute_path

        offers_prepare_options = TMskuKeysExtractorOptions()
        offers_prepare_options.yt.Proxy = self.yt_stuff.get_server()
        for input_offers_path in self.yt_input_offers_path:
            offers_prepare_options.input_offers_table.append(input_offers_path)
        converted_offers_path = ypath_join(get_yt_prefix(), 'input', 'converted_offers')
        converted_offers_path_model_glue = ypath_join(get_yt_prefix(), 'input', 'converted_offers_model_glue')
        offers_prepare_options.output_offers_table = converted_offers_path
        offers_prepare_options.output_offers_table_model_glue = converted_offers_path_model_glue
        cmd = [
            yatest.common.binary_path(os.path.join('market', 'idx', 'offers', 'bin', 'msku_keys_extractor', 'msku_keys_extractor')),
            '--config-text', text_format.MessageToString(offers_prepare_options, as_one_line=True),
        ]
        self.exec_result = self.try_execute_under_gdb(cmd)
        self.verify()
        assert self.yt_stuff.get_yt_client().is_sorted(converted_offers_path_model_glue)

        options = TGlueModelReducerOptions()
        options.yt.Proxy = self.yt_stuff.get_server()
        options.input_offers_table.append(converted_offers_path_model_glue)
        for input_glue_path in self.input_model_glue_yt_paths:
            options.model_glue_table.append(input_glue_path)
        options.output_table = self.yt_output_table

        cmd = [
            binary_path,
            '--config-text', text_format.MessageToString(options, as_one_line=True),
        ]

        self.exec_result = self.try_execute_under_gdb(cmd)

    @property
    def output_table(self):
        return self.yt_output_table
