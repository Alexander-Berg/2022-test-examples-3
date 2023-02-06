#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa
import os
from core.blender_bundles import create_blender_bundles, get_supported_incuts_cgi
from core.paths import BUILDROOT
from core.testcase import TestCase
from google.protobuf.json_format import MessageToJson
from market.media_adv.proto.output.output_incuts_pb2 import (
    EIncutType,
    EMediaElementType,
    TBidInfo,
    TColoredText,
    TConstraints,
    THeader,
    TImage,
    TIncut,
    TMediaElement,
    TModel,
    TVendor,
    TIncutList,
)
from market.media_adv.incut_search.proto.grpc.incuts_grpc_pb2 import TIncutsResponse
from market.pylibrary.lite.process import run_external_tool

# общий файл для тестов МПФ врезок


class TestMediaAdv(TestCase):
    @staticmethod
    def get_request(params, rearr):
        def dict_to_str(data, separator):
            return str(separator).join("{}={}".format(str(k), str(v)) for (k, v) in data.iteritems())

        if params and rearr:
            return "{}&rearr-factors={}".format(dict_to_str(params, '&'), dict_to_str(rearr, ';'))
        elif params:
            return dict_to_str(params, '&')
        else:  # only rearr
            return "rearr-factors={}".format(dict_to_str(rearr, ';'))

    @staticmethod
    def get_params_rearr_factors(request_text, hid):
        params = {
            'place': 'blender',
            'text': request_text,
            'use-default-offers': 1,
            'debug': 'da',
            'allow-collapsing': 1,
            'pp': 18,
            'show-urls': 'productVendorBid,cpa',
            'hid': hid,
            'client': 'frontend',
            'platform': 'desktop',
            'supported-incuts': get_supported_incuts_cgi(
                {"1": ["8", "9", "10", "11", "18"], "2": ["8", "9", "10", "11", "18"]}
            ),  # EIncutViewTypes in library/blender/incut_types.h
        }
        rearr_factors = {
            'market_blender_media_adv_incut_enabled': 1,  # разрешение работы МПФ врезки
            'market_vendor_incut_with_CPA_offers_only': 0,
            'market_vendor_incut_hide_undeliverable_models': 0,
            'market_blender_use_bundles_config': 1,
            'market_media_adv_incut_on_search_place': 0,
        }
        return params, rearr_factors

    @classmethod
    def prepare_blender_bundles_config(cls):
        with open(cls.meta_paths.blender_bundles_config) as bundles_config_file:
            bundles_config = bundles_config_file.read()
        bundles = [
            "const_media_adv_incut_1.json",
            "const_media_adv_incut_search.json",
            "const_media_adv_incut_lowering.json",
            "const_media_adv_incut_search_lowering.json",
        ]
        bundles_data = dict()
        for bundle in bundles:
            with open(os.path.join(cls.meta_paths.blender_bundles, bundle)) as bundle_file:
                bundles_data[bundle] = bundle_file.read()
        cls.settings.formulas_path = create_blender_bundles(cls.meta_paths.testroot, bundles_config, bundles_data)

    @classmethod
    def media_adv_mock(cls, proto_response):
        """
        :param proto_response: TIncut, TIncutList or TIncutsResponse
        """
        use_as_list = False
        use_as_incuts_response = False
        if isinstance(proto_response, TIncut):
            pass
        elif isinstance(proto_response, TIncutList):
            use_as_list = True
        elif isinstance(proto_response, TIncutsResponse):
            use_as_incuts_response = True
        else:
            return ""
        json_str = MessageToJson(proto_response)
        base_path = os.path.join(cls.meta_paths.testroot, "media_adv_mock")
        if not os.path.exists(base_path):
            os.makedirs(base_path)
        in_data_path = os.path.join(base_path, 'in_data')
        out_data_path = os.path.join(base_path, 'out_data')
        log_path = os.path.join(base_path, 'generator.log')
        generator_path = os.path.join(BUILDROOT, 'market/media_adv/tools/madv_output_generator/madv_output_generator')

        with open(in_data_path, 'w') as in_data:
            in_data.write(json_str)

        cmd = [generator_path, '-i', in_data_path, '-o', out_data_path]
        if use_as_list:
            cmd.extend(['-l', '1'])
        elif use_as_incuts_response:
            cmd.extend(['-r', '1'])
        run_external_tool(cmd, log_path, True)  # run tool for create output media_adv message

        with open(out_data_path, 'r') as out_data:
            madv_data_str = out_data.read()
            return madv_data_str

    @classmethod
    def create_incut(cls, models, vendor_id, incut_id, vendor_name=None, datasource_id=28195, header_text='title'):
        if vendor_name is None:
            vendor_name = 'vendor_{}'.format(vendor_id)
        return TIncut(
            Models=[TModel(Id=model_id) for model_id in models],
            Vendor=TVendor(
                Id=vendor_id,
                DatasourceId=datasource_id,
                Name=vendor_name,
            ),
            SaasUrl='saas_url',
            SaasId=incut_id,
            Constraints=TConstraints(
                MinDocs=3,
                MaxDocs=10,
            ),
            BidInfo=TBidInfo(ClickPrice=500, Bid=1200),
            IncutType=EIncutType.ModelsList,
            Header=THeader(
                Type='default',
                Text=header_text,
                Logos=[
                    TMediaElement(
                        Type=EMediaElementType.Logo,
                        Text=TColoredText(
                            Text=header_text,
                        ),
                        Id=761,
                        BidInfo=TBidInfo(
                            ClickPrice=500,
                            Bid=1200,
                        ),
                        SourceImage=TImage(
                            Url='https://avatars.mds.yandex.net/get-mpic/1862933/img_id5477847296568171778.png/orig',
                            Width=800,
                            Height=600,
                        ),
                    )
                ],
            ),
        )

    @classmethod
    def create_incut_response(cls, lists):
        return TIncutsResponse(IncutLists=[TIncutList(Incuts=lst) for lst in lists])
