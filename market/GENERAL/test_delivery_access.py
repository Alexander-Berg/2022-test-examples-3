#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

import test_delivery

from core.testcase import main
from core.response import NullResponse

from market.pylibrary.lite.path import safe_makedirs

import os
import copy
import shutil
import tarfile
import time


class TMarketAccessDelivery(object):
    def __init__(self, test_cls):
        self.__test_cls = test_cls
        self.empty_reportdata = ""
        self.empty_shopindex = ""

    def enable_report_market_dynamic_from_access(self):
        self.__test_cls.settings.market_access_settings.enabled = True
        self.__test_cls.settings.market_access_settings.use_access_delivery_fb_files = True
        self._generate_empty_delivery_files()

    def _get_mds_url(self, shade_host_port, path):
        path = path if path.startswith('/') else '/' + path
        return '{host_port}/mds{path}'.format(
            host_port=shade_host_port,
            path=path,
        )

    def setup_market_access_resources(self, access_server, shade_host_port):
        # At this point we have index files with full delivery data.
        # We are going to replace delivery files with empty data and serve original files with Access.

        # Copy empty regional delivery files to index
        regional_delivery_src_files = ['regional_delivery_modifiers.fb', 'regional_delivery_buckets2.fb']
        for deli_file in regional_delivery_src_files:
            index_file = os.path.join(self.__test_cls.meta_paths.reportdata, deli_file)
            os.rename(index_file, os.path.join(self.__test_cls.meta_paths.access_resources_tmp, deli_file))
            shutil.copy2(os.path.join(self.empty_reportdata, deli_file), index_file)

        # Copy empty delivery info files to index
        delivery_info_src_files = ['offers-hash-mapping.fb', 'offers-delivery-info.fb']
        for deli_file in delivery_info_src_files:
            index_file = os.path.join(self.__test_cls.meta_paths.shopindex, deli_file)
            os.rename(index_file, os.path.join(self.__test_cls.meta_paths.access_resources_tmp, deli_file))
            shutil.copy2(os.path.join(self.empty_shopindex, deli_file), index_file)

        # Make regional Access resource.
        regional_delivery_v1_path = os.path.join(
            self.__test_cls.meta_paths.access_resources, 'regional_delivery_fb/1.0.0/regional_delivery_fb.tar.gz'
        )
        self._make_archive(
            self.__test_cls.meta_paths.access_resources_tmp, regional_delivery_src_files, regional_delivery_v1_path
        )

        # Make delivery info Access resource
        delivery_info_v1_path = os.path.join(
            self.__test_cls.meta_paths.access_resources,
            'offer_delivery_info_fb_00/1.0.0/offer_delivery_info_fb_00.tar.gz',
        )
        self._make_archive(
            self.__test_cls.meta_paths.access_resources_tmp, delivery_info_src_files, delivery_info_v1_path
        )

        # Publish resources
        access_server.create_publisher(name='indexer')
        access_server.create_resource(name='regional_delivery_fb', publisher_name='indexer')
        regional_delivery_v1_url = self._get_mds_url(shade_host_port, regional_delivery_v1_path)
        access_server.create_version('regional_delivery_fb', http_url=regional_delivery_v1_url)

        access_server.create_resource(name='offer_delivery_info_fb_00', publisher_name='indexer')
        delivery_info_v1_url = self._get_mds_url(shade_host_port, delivery_info_v1_path)
        access_server.create_version(
            'offer_delivery_info_fb_00', http_url=delivery_info_v1_url, dependencies=[('regional_delivery_fb', '1.0.0')]
        )

    def _generate_empty_delivery_files(self):
        tmp_index = copy.deepcopy(self.__test_cls.index)
        self.empty_reportdata = os.path.join(tmp_index.paths.tmproot, "report-data-empty")
        self.empty_shopindex = os.path.join(tmp_index.paths.tmproot, 'part-0')
        shutil.copytree(tmp_index.paths.shopindex, self.empty_shopindex)
        tmp_index.paths.reportdata = self.empty_reportdata
        tmp_index.paths.shopindex = self.empty_shopindex
        tmp_index.commit()

    def _make_archive(self, src_dir, files_list, archive_dst):
        archive_dir = os.path.dirname(archive_dst)
        safe_makedirs(archive_dir)
        with tarfile.open(archive_dst, 'w:gz') as tar:
            old_cwd = os.getcwd()
            os.chdir(src_dir)
            try:
                for filename in files_list:
                    tar.add(filename)
            except:
                raise
            finally:
                os.chdir(old_cwd)

    def wait_market_access(self, test_obj):
        for _ in range(30):
            response = test_obj.report.request_xml('admin_action=versions&aquirestats=1')
            if not isinstance(response, NullResponse):
                regional_ok, _ = response.contains(
                    '''<regional_delivery_fb>1.0.0</regional_delivery_fb>''', False, True, False
                )

                # FIXME: see https://st.yandex-team.ru/MSI-383 for details
                #
                # delivery_info_ok, _ = response.contains(
                #     '''<offer_delivery_info_fb_00>1.0.0</offer_delivery_info_fb_00>''', False, True, False
                # )
                # if regional_ok and delivery_info_ok:
                #     break

                if regional_ok:
                    break
                time.sleep(1)
        response = test_obj.report.request_xml('admin_action=versions&aquirestats=1')
        test_obj.assertFragmentIn(response, '''<regional_delivery_fb>1.0.0</regional_delivery_fb>''')

        # FIXME: see https://st.yandex-team.ru/MSI-383 for details
        #
        # test_obj.assertFragmentIn(response, '''<offer_delivery_info_fb_00>1.0.0</offer_delivery_info_fb_00>''')


class T(test_delivery.T):
    access_driver = None

    @classmethod
    def prepare(cls):
        cls.access_driver = TMarketAccessDelivery(cls)
        cls.access_driver.enable_report_market_dynamic_from_access()
        cls.settings.enable_access_delivery_for_search = True

        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']

    @classmethod
    def setup_market_access_resources(cls, access_server, shade_host_port):
        cls.access_driver.setup_market_access_resources(access_server, shade_host_port)

    def test_access_loads_fb_delivery_files(self):
        self.__class__.access_driver.wait_market_access(self)


if __name__ == '__main__':
    main()
