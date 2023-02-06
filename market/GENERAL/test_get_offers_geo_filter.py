#!/usr/bin/env python
# -*- coding: utf-8 -*
import __classic_import     # noqa
import market.click_n_collect.mt.env as env

import market.pylibrary.lite.test_platform as test_platform
import json
import os
import subprocess


class T(env.TestSuite):

    @classmethod
    def get_request(cls, request={}):
        default = {
            'clickNCollectId': '1',
        }
        default.update(request)
        return default

    @classmethod
    def prepare(cls):
        cls.click_n_collect.with_tvmapi = True
        cls.click_n_collect.config.ServerTvm.RemoteId.extend([cls.tvm_client_id])

        if test_platform.is_yatest():
            geodata_dir = os.path.join(cls.click_n_collect.ctx.build_root, 'geobase/data/v6')
        else:
            geodata_dir = cls.click_n_collect.ctx.work_dir.path
            subprocess.check_call(['sky', 'get', '--dir', geodata_dir, 'sbr:1774963827'])

        cls.click_n_collect.config.Geodata6BinPath = os.path.join(geodata_dir, 'geodata6.bin')

        cls.click_n_collect.with_goods_server = True
        cls.click_n_collect.config.GetOffersHandler = True
        cls.click_n_collect.config.GoodsRequester.Host = ''         # it will be filled by beam

        cls.click_n_collect.goods_server.config.Outlets.extend([
            # somewhere in moscow
            cls.get_goods_outlet('1', 1, latlon=[' 55.754231', '37.620004']),

            # definitely not in moscow
            cls.get_goods_outlet('1', 2, latlon=['53.145592', '29.225538']),
        ])

    def test_filter_non_moscow(self):
        """ Check that for Moscow (213) and parent region Moscow oblast (1) we leave outlet #1 and remove outlet #2 """
        region_ids = [213, 1]
        for region_id in region_ids:
            request = T.get_request({'filters': {'geo': {'includeIds': [region_id]}}})
            response = self.click_n_collect.request_json(
                'get_offers',
                method='POST',
                body=json.dumps(request),
                headers=T.get_headers())
            self.assertFragmentIn(
                response,
                {
                    'success': True,
                    'data': [
                        {
                            'goodsId': "1",
                            'location': {
                                'identification': {'id': '1'}
                            }
                        },
                    ],
                },
                allow_different_len=False
            )

    def test_filter_moscow(self):
        """ Check that when excluding Moscow (213) and parent region Moscow oblast (1) we leave outlet #2
            and remove outlet #1 """

        region_ids = [213, 1]
        for region_id in region_ids:
            request = T.get_request({'filters': {'geo': {'excludeIds': [region_id]}}})
            response = self.click_n_collect.request_json(
                'get_offers',
                method='POST',
                body=json.dumps(request),
                headers=T.get_headers())
            self.assertFragmentIn(
                response,
                {
                    'success': True,
                    'data': [
                        {
                            'goodsId': "1",
                            'location': {
                                'identification': {'id': '2'}
                            }
                        },
                    ],
                },
                allow_different_len=False
            )


if __name__ == '__main__':
    env.main()
