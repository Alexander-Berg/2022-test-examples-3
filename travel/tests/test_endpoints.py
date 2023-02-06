# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import socket

import mock

from travel.library.python.yp.endpoints import YpEndpoints
from infra.yp_service_discovery.api.api_pb2 import TReqResolveEndpoints, TRspResolveEndpoints, TEndpointSet, TEndpoint
from infra.yp_service_discovery.python.resolver.resolver import Resolver


class TestYpEndpoints(object):
    def test_get_endpoints_for_dc(self):
        yp_endpoints = YpEndpoints('myclient', 'endpointset42')

        with mock.patch.object(Resolver, 'resolve_endpoints', autospec=True) as m_resolve_endpoints:
            m_resolve_endpoints.return_value = mock.MagicMock()
            assert yp_endpoints.get_endpoints_for_dc('vla') is m_resolve_endpoints.return_value

            call_request = m_resolve_endpoints.call_args_list[0][0][1]
            assert isinstance(call_request, TReqResolveEndpoints)

            assert call_request == TReqResolveEndpoints(
                endpoint_set_id='endpointset42',
                cluster_name='vla',
            )

            hostname = socket.gethostname()
            assert yp_endpoints.resolver._client_name == 'myclient:{}'.format(hostname)

    def test_get_endpoints(self):
        yp_endpoints = YpEndpoints('myclient', 'endpointset42', datacenters=['iva', 'man', 'sas'])

        def m_resolve_endpoints_call(self, request):
            assert self is yp_endpoints.resolver
            dc = request.cluster_name
            return mock.MagicMock(dc='hosts_for_{}'.format(dc))

        with mock.patch.object(Resolver, 'resolve_endpoints', autospec=True) as m_resolve_endpoints:
            m_resolve_endpoints.side_effect = m_resolve_endpoints_call

            endpoint_by_dc = yp_endpoints.get_endpoints()
            assert len(endpoint_by_dc) == 3
            assert endpoint_by_dc['iva'].dc == 'hosts_for_iva'
            assert endpoint_by_dc['man'].dc == 'hosts_for_man'
            assert endpoint_by_dc['sas'].dc == 'hosts_for_sas'

    def test_get_hosts(self):
        yp_endpoints = YpEndpoints('myclient', 'endpointset42', datacenters=['iva', 'man'])

        def m_resolve_endpoints_call(self, request):
            assert self is yp_endpoints.resolver
            dc = request.cluster_name
            return TRspResolveEndpoints(
                endpoint_set=TEndpointSet(
                    endpoints=[
                        TEndpoint(
                            fqdn='host_{}_{}'.format(dc, i),
                            ready=i != 1
                        )
                        for i in range(3)
                    ]
                )
            )

        with mock.patch.object(Resolver, 'resolve_endpoints', autospec=True) as m_resolve_endpoints:
            m_resolve_endpoints.side_effect = m_resolve_endpoints_call

            hosts = yp_endpoints.get_hosts()
            assert hosts == ['host_iva_0', 'host_iva_2', 'host_man_0', 'host_man_2']

    def test_get_ip6_addresses_by_dc(self):
        yp_endpoints = YpEndpoints('myclient', 'endpointset42', datacenters=['iva', 'man'])

        def m_resolve_endpoints_call(self, request):
            assert self is yp_endpoints.resolver
            dc = request.cluster_name
            return TRspResolveEndpoints(
                endpoint_set=TEndpointSet(
                    endpoints=[
                        TEndpoint(
                            fqdn='host_{}_{}'.format(dc, i),
                            ip6_address='2a02:6b8:c14:5295:0:424a:{}:{}'.format(dc, i),
                            ready=i != 1
                        )
                        for i in range(3)
                    ]
                )
            )

        with mock.patch.object(Resolver, 'resolve_endpoints', autospec=True) as m_resolve_endpoints:
            m_resolve_endpoints.side_effect = m_resolve_endpoints_call

            addresses_by_dc = yp_endpoints.get_ip6_addresses_by_dc()
            assert addresses_by_dc == {
                'iva': ['2a02:6b8:c14:5295:0:424a:iva:0', '2a02:6b8:c14:5295:0:424a:iva:2'],
                'man': ['2a02:6b8:c14:5295:0:424a:man:0', '2a02:6b8:c14:5295:0:424a:man:2'],
            }
