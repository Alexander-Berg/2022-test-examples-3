#!/usr/bin/env python
# -*- coding: utf-8 -*
import __classic_import     # noqa
import market.click_n_collect.mt.env as env
from market.click_n_collect.mock.laas.proto.config_pb2 import TConfig
from market.pylibrary.lite.matcher import Contains, Regex, NotEmpty
import json


class T(env.TestSuite):

    @classmethod
    def get_request(cls, request={}):
        default = {
            'clickNCollectId': '1',
            'userInfo': {
                'ip': '127.0.0.1',
                'yandexUid': '1'
            }
        }
        default.update(request)
        return default

    @classmethod
    def get_statistics_request(cls, request={}):
        default = {
            'clickNCollectIds': ['1', '3', '4'],
            'isStatisticsRequest': True
        }
        default.update(request)
        return default

    @classmethod
    def prepare(cls):
        cls.click_n_collect.with_tvmapi = True
        cls.click_n_collect.config.ServerTvm.RemoteId.extend([cls.tvm_client_id])

        cls.click_n_collect.with_goods_server = True
        cls.click_n_collect.config.GetOffersHandler = True
        cls.click_n_collect.config.GoodsRequester.Host = ''         # it will be filled by beam

        for i in range(3):
            latlon = (' ' + str(20 - i * 10), str(20 - i * 10) + ' ')
            cls.click_n_collect.goods_server.config.Outlets.extend([cls.get_goods_outlet('1', i, latlon=latlon)])

        cls.click_n_collect.goods_server.config.Outlets.extend([
            # to test aggregation we will add another outlet with id 2
            cls.get_goods_outlet('1', 2, latlon=['0', '0']),

            # test that outlet with incorrect latlon will be filtered out
            cls.get_goods_outlet('1', 3, latlon=['', 'asdf']),

            # to test statistics request with making unique outlets
            cls.get_goods_outlet('3', 2, latlon=['0', '0']),
            cls.get_goods_outlet('3', 2, latlon=['0', '0']),
            cls.get_goods_outlet('3', 3, latlon=['0', '0']),

            cls.get_goods_outlet('4', 2, latlon=['0', '0']),
            cls.get_goods_outlet('4', 2, latlon=['0', '0']),
            cls.get_goods_outlet('4', 3, latlon=['0', '0']),
        ])

        for i in range(2):
            latlon = (' ' + str(20 - i * 10), str(20 - i * 10) + ' ')
            cls.click_n_collect.goods_server.config.Outlets.extend([cls.get_goods_outlet('2', 10+i, latlon=latlon)])

        service_id = 'zuppa-service'
        cls.click_n_collect.with_laas = True
        cls.click_n_collect.config.Laas.ServiceId = service_id
        cls.click_n_collect.config.LaasRequester.Host = ''    # it will be filled by beam
        cls.click_n_collect.laas.config.ServiceIds.extend([service_id])

        location = TConfig.TLocation()
        location.UserInfo.YandexUid = '1'
        location.UserInfo.Ip = '127.0.0.1'
        location.Response.Latitude = 0
        location.Response.Longitude = 0
        cls.click_n_collect.laas.config.Locations.extend([location])

        location = TConfig.TLocation()
        location.UserInfo.YandexUid = '2'
        location.UserInfo.YandexGid = 2
        location.Response.Latitude = 30
        location.Response.Longitude = 30
        cls.click_n_collect.laas.config.Locations.extend([location])

    def test_old_scheme(self):
        headers = T.get_headers()
        request = T.get_request()
        response = self.click_n_collect.request_json('get_offers', method='POST', body=json.dumps(request), headers=headers)
        self.assertFragmentIn(
            response,
            {
                'success': True,
                'data': [
                    {
                        'goodsId': "1",
                        'price': 100,
                        'remainQty': 1,
                        'location': {
                            'identification': {
                                'id': '0',
                                'externalId': '7777777',
                            }
                        }
                    },
                    {
                        'goodsId': "1",
                        'price': 101,
                        'remainQty': 2,
                        'location': {
                            'identification': {
                                'id': '1',
                                'externalId': '7777777',
                            }
                        }
                    },
                    {
                        'goodsId': "1",
                        'price': 102,
                        'remainQty': 6,     # 4 since we aggregated two outltes with same price & id
                        'location': {
                            'identification': {
                                'id': '2',
                                'externalId': '7777777',
                            }
                        }
                    },
                ],
                'user': {
                    'location': {
                        'latitude': 0,
                        'longitude': 0,
                    }
                }
            },
            preserve_order=False,
            allow_different_len=False
        )

    def test_statistics_request(self):
        headers = T.get_headers()
        request = T.get_statistics_request()
        response = self.click_n_collect.request_json('get_offers', method='POST', body=json.dumps(request), headers=headers)
        self.assertFragmentIn(
            response,
            {
                'success': True,
                'goodsId2OutletsIds': {
                    '1': {
                        'outletIds': {
                            '0': 1,
                            '1': 2,
                            '2': 6
                        }
                    },
                    '3': {
                        'outletIds': {
                            '2': 6,
                            '3': 4
                        }
                    },
                    '4': {
                        'outletIds': {
                            '2': 6,
                            '3': 4
                        }
                    }
                }
            },
            preserve_order=False,
            allow_different_len=False
        )

    def test_outlet_geo_sorting(self):
        headers = T.get_headers()

        request = T.get_request()
        response = self.click_n_collect.request_json('get_offers', method='POST', body=json.dumps(request), headers=headers)
        self.assertFragmentIn(
            response,
            {
                'success': True,
                'user': {
                    'location': {
                        'latitude': 0,
                        'longitude': 0,
                    },
                },
                'data': [
                    {
                        'location': {
                            'identification': {
                                'id': '2',
                            },
                            'location': {
                                'geo': {'lon': '0 ', 'lat': ' 0'}
                            }
                        }
                    },
                    {
                        'location': {
                            'identification': {
                                'id': '1',
                            },
                            'location': {
                                'geo': {'lon': '10 ', 'lat': ' 10'}
                            }
                        }
                    },
                    {
                        'location': {
                            'identification': {
                                'id': '0',
                            },
                            'location': {
                                'geo': {'lon': '20 ', 'lat': ' 20'}
                            }
                        }
                    },
                ],
            },
            preserve_order=True,
            allow_different_len=False
        )

        request = T.get_request({'userInfo': {'yandexUid': '2', 'yandexGid': 2}})
        response = self.click_n_collect.request_json('get_offers', method='POST', body=json.dumps(request), headers=headers)
        self.assertFragmentIn(
            response,
            {
                'success': True,
                'user': {
                    'location': {
                        'latitude': 30,
                        'longitude': 30,
                    },
                },
                'data': [
                    {
                        'location': {
                            'identification': {
                                'id': '0',
                            },
                            'location': {
                                'geo': {'lon': '20 ', 'lat': ' 20'}
                            }
                        }
                    },
                    {
                        'location': {
                            'identification': {
                                'id': '1',
                            },
                            'location': {
                                'geo': {'lon': '10 ', 'lat': ' 10'}
                            }
                        }
                    },
                    {
                        'location': {
                            'identification': {
                                'id': '2',
                            },
                            'location': {
                                'geo': {'lon': '0 ', 'lat': ' 0'}
                            }
                        }
                    },
                ],
            },
            preserve_order=True,
            allow_different_len=False
        )

        request = T.get_request()
        del request['userInfo']
        response = self.click_n_collect.request_json('get_offers', method='POST', body=json.dumps(request), headers=headers)
        self.assertFragmentIn(
            response,
            {
                'success': True,
                'data': [
                    {
                        'location': {
                            'identification': {
                                'id': '0',
                            },
                            'location': {
                                'geo': {'lon': '20 ', 'lat': ' 20'}
                            }
                        }
                    },
                    {
                        'location': {
                            'identification': {
                                'id': '1',
                            },
                            'location': {
                                'geo': {'lon': '10 ', 'lat': ' 10'}
                            }
                        }
                    },
                    {
                        'location': {
                            'identification': {
                                'id': '2',
                            },
                            'location': {
                                'geo': {'lon': '0 ', 'lat': ' 0'}
                            }
                        }
                    },
                ],
            },
            preserve_order=False,
            allow_different_len=False
        )
        self.assertFragmentNotIn(
            response,
            {
                'userLocation': NotEmpty()
            },
        )

    def test_invalid_request(self):
        headers = T.get_headers()

        response = self.click_n_collect.request_json('get_offers', method='POST', body='asdf', headers=headers, fail_on_error=False)
        self.assertEqual(response.code, 400)
        self.assertFragmentIn(
            response,
            {
                'status': 'error',
                'message': Regex('can\'t parse request body.*Invalid value')
            }
        )

        response = self.click_n_collect.request_json('get_offers', method='POST', body=json.dumps({}), headers=headers, fail_on_error=False)
        self.assertEqual(response.code, 400)
        self.assertFragmentIn(
            response,
            {
                'status': 'error',
                'message': Regex('either \'clickNCollectId\' or \'clickNCollectIds\' field should present')
            }
        )

        response = self.click_n_collect.request_json('get_offers', method='POST', body=json.dumps({'clickNCollectId': ''}), headers=headers, fail_on_error=False)
        self.assertEqual(response.code, 400)
        self.assertFragmentIn(
            response,
            {
                'status': 'error',
                'message': Regex('can\'t parse request body.*\'clickNCollectId\' field should not be emtpy')
            }
        )

        T.common_log.expect(Contains('Error response:'), 'ERRR')

    def test_tvm_ticket(self):
        request = T.get_request()

        T.common_log.expect(Contains('Error response:'), 'ERRR')
        T.common_log.expect(Contains('get_offers: someone came without "X-Ya-Service-Ticket" header'), 'WARN')
        response = self.click_n_collect.request_json('get_offers', method='POST', body=json.dumps(request), fail_on_error=False)
        self.assertEqual(response.code, 400)
        self.assertFragmentIn(
            response,
            {
                'status': 'error',
                'message': 'no "X-Ya-Service-Ticket" header'
            }
        )

        T.common_log.expect(Contains('Error response:'), 'ERRR')
        T.common_log.expect(Contains('get_offers: TVM ticket check failed: Malformed ticket'), 'ERRR')
        response = self.click_n_collect.request_json('get_offers', method='POST', body=json.dumps(request), headers={'X-Ya-Service-Ticket': ''}, fail_on_error=False)
        self.assertEqual(response.code, 400)
        self.assertFragmentIn(
            response,
            {
                'status': 'error',
                'message': 'TVM ticket check failed: Malformed ticket'
            }
        )


if __name__ == '__main__':
    env.main()
