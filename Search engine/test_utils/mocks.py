# coding: utf-8

"""
FIXME
"""

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from search.priemka.yappy.proto.structures.sandbox_pb2 import SandboxFileList
from search.priemka.yappy.services.yappy.services.Model import ModelClient


class ModelClientMock(ModelClient):
    # noinspection PyMissingConstructor
    def __init__(self):
        pass

    def list_sandbox_files(self, request, timeout=0.1, metadata=None, credentials=None):
        return SandboxFileList()

    def batch_update_sandbox_files(self, request, timeout=0.1, metadata=None, credentials=None):
        return request
