# coding: utf-8

import os
import yatest.common
from market.idx.yatf.matchers.env_matchers import HasExitCode
from market.idx.yatf.test_envs.base_env import BaseEnv
from market.idx.yatf.resources.resource import FileResource

_STUBS_DIR = os.path.join(yatest.common.source_path(), 'market', 'tools',
                          'wizard-model-reviews-json2mmap-converter', 'yatf',
                          'resources', 'stubs')


class WizardModelReviewsTestEnv(BaseEnv):

    _MATCHERS = [
        HasExitCode(0)
    ]

    _STUBS = {
        name: FileResource(os.path.join(_STUBS_DIR, filename))
        for name, filename in {
            'model_wizard_reviews_json': 'model_wizard_reviews.json',
        }.items()
    }

    @property
    def description(self):
        return 'wizard-model-reviews-converter'

    def __init__(self, **resources):
        super(WizardModelReviewsTestEnv, self).__init__(**resources)

    def execute(self, path=None):
        if path is None:
            relative_path = os.path.join('market', 'tools',
                                         'wizard-model-reviews-json2mmap-converter',
                                         'wizard-model-reviews-json2mmap-converter')
            absolute_path = yatest.common.binary_path(relative_path)
            path = absolute_path

        model_wizard_reviews_json_path = os.path.join(
            self.input_dir,
            self.resources['model_wizard_reviews_json'].filename
        )
        model_wizard_reviews_mmap_path = os.path.join(
            self.output_dir,
            'best_grades_for_koldunshik.mmap'
        )

        cmd = [
            path,
            model_wizard_reviews_json_path,
            model_wizard_reviews_mmap_path
        ]
        self.exec_result = self.try_execute_under_gdb(
            cmd,
            cwd=self.output_dir,
            check_exit_code=False
        )

        self.outputs.update({
            'model_wizard_reviews_mmap': FileResource(
                model_wizard_reviews_mmap_path
            ),
        })
