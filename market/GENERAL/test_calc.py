#!/usr/bin/env python
# -*- coding: utf-8 -*-
import os
from core.testcase import TestCase, main    # noqa
import core.testenv as testenv              # noqa
from market.library.ml_model.calc_service.proto.CalcService_pb2 import Request as RequestProto, Response as ResponseProto



class T(TestCase):
    @classmethod
    def prepare_all(cls, resources, report):
        src = testenv.get_source_path("market/forecaster/lite/data/catboost-models")
        dst = testenv.get_paths().calc_models
        os.symlink(src, dst)

    def calc(self, request_proto):
        import base64, zlib

        compressed = zlib.compress(request_proto.SerializeToString())
        encoded = base64.b64encode(compressed)

        response = self.service.request_text(name="calc", method="POST", body=encoded)

        decoded = base64.urlsafe_b64decode('{}=='.format(str(response).rstrip(',')))
        decompressed = zlib.decompress(decoded)

        response_proto = ResponseProto()
        response_proto.ParseFromString(decompressed)
        return response_proto

    def test_handle_description(self):
        """ Check if the handle exists and provides expected description
        """
        response = self.service.request_json(name="calc", params="help")
        self.assertFragmentIn(response, {
            "Description": "CatBoost model calculator",
        })

    def test_weather_model(self):
        req = RequestProto()
        req.model = "weather.cbm"

        obj = req.objects.add()
        obj.num_factors.extend([10, 5, 753])
        obj.cat_factors.append("north")

        obj = req.objects.add()
        obj.num_factors.extend([30, 1, 760])
        obj.cat_factors.append("south")

        obj = req.objects.add()
        obj.num_factors.extend([14, 2.5, 725])
        obj.cat_factors.append("west")

        resp = self.calc(req)
        self.assertEqual(len(resp.values), 3)
        self.assertAlmostEqual(resp.values[0], 1, 1)
        self.assertAlmostEqual(resp.values[1], 0, 1)
        self.assertAlmostEqual(resp.values[2], 1, 1)


if __name__ == '__main__':
    main()

