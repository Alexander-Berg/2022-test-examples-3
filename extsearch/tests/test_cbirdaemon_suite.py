#!/usr/bin/python
# -*- coding: utf-8 -*-

import yatest.common

import json
import pytest


class CbirdaemonSuite():
    def setup_class(cls):
        cls.test_env = None

    def test_image(cls):
        result = cls.send_request(yatest.common.work_path("image_to_test.jpg"))
        out_file_path = "image.%s.out" % cls.test_env
        with open(out_file_path, "w") as out_file:
            out_file.write(json.dumps(result["json"], indent=1, sort_keys=True))
        return yatest.common.canonical_file(out_file_path, diff_tool=cls.get_diff_tool())

    def test_image_2faces(cls):
        result = cls.send_request(yatest.common.work_path("2faces.jpg"))
        out_file_path = "image.2faces.%s.out" % cls.test_env
        with open(out_file_path, "w") as out_file:
            out_file.write(json.dumps(result["json"], indent=1, sort_keys=True))
        return yatest.common.canonical_file(out_file_path, diff_tool=[cls.get_diff_tool(), "-t", "1e-3"])

    def test_image_car(cls):
        result = cls.send_request(yatest.common.work_path("car.jpg"))
        out_file_path = "image.car.%s.out" % cls.test_env
        with open(out_file_path, "w") as out_file:
            out_file.write(json.dumps(result["json"], indent=1, sort_keys=True))
        return yatest.common.canonical_file(out_file_path, diff_tool=cls.get_diff_tool())

    def test_image_face(cls):
        result = cls.send_request(yatest.common.work_path("face.jpg"))
        out_file_path = "image.face.%s.out" % cls.test_env
        with open(out_file_path, "w") as out_file:
            out_file.write(json.dumps(result["json"], indent=1, sort_keys=True))
        return yatest.common.canonical_file(out_file_path, diff_tool=[cls.get_diff_tool(), "-t", "1e-3"])

    def test_image_market(cls):
        result = cls.send_request(yatest.common.work_path("market.jpg"))
        out_file_path = "image.market.%s.out" % cls.test_env
        with open(out_file_path, "w") as out_file:
            out_file.write(json.dumps(result["json"], indent=1, sort_keys=True))
        return yatest.common.canonical_file(out_file_path, diff_tool=cls.get_diff_tool())

    @pytest.mark.trylast
    def test_stop_cbirdaemon(cls):
        return cls.call_stop_cbirdaemon()
