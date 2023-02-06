# coding: utf-8
import os
import yatest.common

from market.idx.yatf.matchers.env_matchers import HasExitCode
from market.idx.yatf.resources.resource import FileResource
from market.idx.yatf.test_envs.base_env import BaseEnv

from market.idx.yatf.resources.yt_table_resource import YtTableResource
from market.idx.yatf.resources.tovar_tree_pb import TovarTreePb, MboCategory

_STUBS_DIR = os.path.join(yatest.common.source_path(), 'market', 'idx', 'tools', 'market_yt_data_upload',
                          'yatf', 'resources', 'stubs')


def make_categories():
    return [
        MboCategory(hid=90401, tovar_id=0,
                    unique_name="Все товары", name="Все товары",
                    aliases=["Все товары"], no_search=True,
                    output_type=MboCategory.SIMPLE),
        MboCategory(hid=198118, tovar_id=1523, parent_hid=90401,
                    name='Бытовая техника', unique_name='Бытовая техника',
                    output_type=MboCategory.SIMPLE, aliases=['Побутова техніка']),
    ]


class YtDataUploadTestEnv(BaseEnv):
    _MATCHERS = [
        HasExitCode(0)
    ]

    _STUBS = {
        name: FileResource(os.path.join(_STUBS_DIR, filename))
        for name, filename in {
            'model': 'models_90592.pb',
            'vendor': 'global.vendors.xml',
            'parameters': 'parameters_90592.pb',
            'shopsdat': 'shops-utf8.dat',
            'crs': 'category_region_stats.csv',
            'navigation': 'cataloger.navigation.all.xml',
            'navigation_deprecated': 'cataloger.navigation.xml',
        }.items()
    }

    # Full tovar-tree.pb is too large
    _STUBS['categories'] = TovarTreePb(make_categories())

    def __init__(self, **resources):
        super(YtDataUploadTestEnv, self).__init__(**resources)

    @property
    def description(self):
        return 'yt_data_uploader'

    def execute(self, yt_stuff, type, output_table, input_table=None, path=None, use_old_stub=False, second_input_table=None):
        if path is None:
            relative_path = os.path.join('market', 'idx', 'tools', 'market_yt_data_upload', 'src', 'market-yt-data-upload')
            absolute_path = yatest.common.binary_path(relative_path)
            path = absolute_path

        if type == 'mrs' or type == 'grs' or type == 'model_yt'or type == 'sku_export_yt' or type == 'sku_availability_yt' or type == 'mbo_rendered_description':
            input_path = input_table
        else:
            input_path = os.path.join(self.input_dir, self.resources[type].filename)

        proxy = yt_stuff.get_server()

        # TODO: (MARKETINDEXER-26639) убрать после перехода
        cmd = [
            path,
            '--type', type,
            '--input', input_path,
            '--destination', output_table,
            '--proxy', proxy,
        ]

        if type == 'mbo_rendered_description' or type == 'sku_availability_yt':
            cmd.extend(['--input', second_input_table])

        # TODO: (MARKETINDEXER-26639) убрать после перехода
        if type == 'categories':
            stubname = "navigation_deprecated" if use_old_stub else "navigation"
            navigation_forest_path = os.path.join(self.input_dir, self.resources[stubname].filename)
            cmd.extend(['--input', navigation_forest_path, ])

        self.exec_result = self.try_execute_under_gdb(
            cmd,
            cwd=self.output_dir,
            check_exit_code=True
        )

        self.outputs.update(
            {
                "result_table": YtTableResource(yt_stuff, output_table, load=True)
            }
        )
