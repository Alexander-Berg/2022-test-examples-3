# coding: utf-8
import os
import yatest.common
from market.idx.yatf.test_envs.base_env import BaseEnv


class PromoSecondaryOffersConvertor(BaseEnv):
    OUTPUT_FILENAME = 'promo_secondary_offers.mmap'

    def __init__(self, **resources):
        super(PromoSecondaryOffersConvertor, self).__init__(**resources)
        self.output_path = os.path.join(self.output_dir, PromoSecondaryOffersConvertor.OUTPUT_FILENAME)

    @property
    def description(self):
        return 'promo-secondary-offers-converter'

    def execute(self):
        relative_bin_path = os.path.join(
            'market',
            'tools',
            'promo_secondary_offers_converter',
            'bin',
            'promo-secondary-offers-converter',
        )
        absolute_bin_path = yatest.common.binary_path(relative_bin_path)
        input_file = self.resources['input_json_file']
        args = [
            absolute_bin_path,
            '-i',       input_file.path,
            '-o',       self.output_path
        ]
        self.exec_result = self.try_execute_under_gdb(
            args,
            cwd=self.output_dir,
        )
