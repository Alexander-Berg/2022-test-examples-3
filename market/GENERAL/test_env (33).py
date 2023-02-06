# coding: utf-8
import os
import yatest.common

from market.idx.yatf.matchers.env_matchers import HasExitCode
from market.idx.yatf.resources.resource import FileResource
from market.idx.yatf.test_envs.base_env import BaseEnv

from market.idx.yatf.resources.yt_table_resource import YtDynTableResource


def _STUBS_DIR():
    return os.path.join(
        yatest.common.source_path(),
        'market',
        'tools',
        'msku-uploader',
        'yatf',
        'resources',
        'stubs'
    )


class MskuUploaderTestEnv(BaseEnv):

    _MATCHERS = [
        HasExitCode(0)
    ]

    def __init__(
            self,
            jump_table_path='',
            contex_table_path='',
            contex_enabled=False,
            use_pokupki_domain=False,
            filter_out_unpublished_msku=True,
            yt_input_dir=None,
            use_original_by_link=False,
            **resources
    ):
        self._STUBS.update({
            name: FileResource(os.path.join(_STUBS_DIR(), filename))
            for name, filename in list({
                'shopsdat': 'shops-utf8.dat',
            }.items())
        })
        super(MskuUploaderTestEnv, self).__init__(**resources)
        self.output_yt_ext_path = '//home/test/msku_ext'
        self.output_yt_path = '//home/test/msku'
        self.output_contex_path = contex_table_path
        self.output_jump_table_path = jump_table_path
        self.yt_bundle = 'default'
        self.yt_primary_medium = 'default'
        self.yt_input_dir = yt_input_dir
        self.exec_result = None
        self.contex_enabled = contex_enabled
        self.use_pokupki_domain = use_pokupki_domain
        self.filter_out_unpublished_msku = filter_out_unpublished_msku
        self.use_original_by_link = use_original_by_link

    @property
    def description(self):
        return 'msku-uploader'

    def execute(self, yt_stuff):
        relative_bin_path = os.path.join(
            'market',
            'tools',
            'msku-uploader',
            'bin',
            'msku-uploader'
        )
        absolute_bin_path = yatest.common.binary_path(relative_bin_path)

        yt = yt_stuff.get_yt_client()

        yt.remove(self.output_yt_path, recursive=True, force=True)
        yt.remove(self.output_yt_ext_path, recursive=True, force=True)

        if self.output_contex_path:
            yt.remove(self.output_contex_path, recursive=True, force=True)

        if self.output_jump_table_path:
            yt.remove(self.output_jump_table_path, recursive=True, force=True)

        args = [
            absolute_bin_path,
            '--mbo-input',                  self.input_dir,
            '--shopsdat',                   self.resources['shopsdat'].path,
            '--yt-proxy',                   yt_stuff.get_server(),
            '--yt-cargo-types-map-table',   '//home/test/mbo_id_to_cargo_type',
            '--yt-msku-ext-output-table',   self.output_yt_ext_path,
            '--yt-jump-table-output-table', self.output_jump_table_path,
            '--yt-output-table',            self.output_yt_path,
            '--yt-bundle',                  self.yt_bundle,
            '--yt-primary-medium',          self.yt_primary_medium,
            '--yt-contex-output-table',     self.output_contex_path,
            '--enable-contex',              str(self.contex_enabled).lower(),
            '--use-pokupki-domain',         str(self.use_pokupki_domain).lower(),
            '--filter-out-unpublished-msku', str(self.filter_out_unpublished_msku).lower(),
            '--use-original-by-link',       str(self.use_original_by_link).lower(),
        ]

        if 'contex_relations' in self.resources:
            args.extend([
                '--contex-relations-file-path',
                self.resources.get('contex_relations').path,
            ])

        if self.yt_input_dir:
            args.extend(['--yt-mbo-input', self.yt_input_dir])

        self.exec_result = self.try_execute_under_gdb(
            args,
            cwd=self.output_dir,
            check_exit_code=True
        )
        data_to_update = {
            "result_ext_table": YtDynTableResource(
                yt_stuff,
                self.output_yt_ext_path,
                load=True
            ),
            "result_table": YtDynTableResource(
                yt_stuff,
                self.output_yt_path,
                load=True
            ),
        }
        if self.output_contex_path:
            data_to_update['result_contex_table'] = YtDynTableResource(
                yt_stuff,
                self.output_contex_path,
                load=True
            )
        if self.output_jump_table_path:
            data_to_update['jump_table'] = YtDynTableResource(
                yt_stuff,
                self.output_jump_table_path,
                load=True
            )
        self.outputs.update(data_to_update)
