import json
import market.search.parametrizator.mt.env as env

from google.protobuf.json_format import MessageToDict
from market.search.parametrizator.mt.env.hypercategory import HyperCategoryTree, HyperCategory
from market.search.parametrizator.mt.env.gltype import GLParam, GLParamStorage
from market.library.shiny.external.parametrizator.proto.parametrizator_pb2 import (
    ParametrizatorRequest,
    ParametrizatorResponse,
    GlParamValue,
    GlParam,
    GlValue,
    Extension,
)
from market.pylibrary.lite.matcher import Regex
from market.pylibrary.mocks.formalizer import FormalizedParam


class Request(object):
    def __init__(self, query, hid):
        self.request = ParametrizatorRequest(query=query, category_id=hid)

    def body(self):
        return self.request.SerializeToString()

    def json(self):
        return json.dumps(MessageToDict(self.request, preserving_proto_field_name=True), separators=(',', ':'))


class T(env.ParametrizatorSuite):
    @classmethod
    def prepare(cls):
        cls.hypertree = HyperCategoryTree(
            [
                HyperCategory(hid=12, name="black tea"),
                HyperCategory(hid=13, name="xiaomi"),
                HyperCategory(hid=14, name="iphone"),
                HyperCategory(hid=15, name="iphone from old handler"),
                HyperCategory(hid=16, name="fashion iphone from new handler"),
                HyperCategory(hid=17, name="no fashion iphone from new handler"),
            ]
        )
        cls.save_tovar_tree()

        cls.gltypes = GLParamStorage(
            [
                GLParam(hid=12, param_id=4),
                GLParam(hid=13, param_id=7893318),
                GLParam(hid=14, param_id=5),
                GLParam(hid=15, param_id=6),
                GLParam(hid=16, param_id=7),
                GLParam(hid=17, param_id=7),
            ]
        )
        cls.save_gl_types()

        cls.parametrizator.formalizer.on_request(hid=12, query='black tea').respond(
            formalized_params=[
                FormalizedParam(
                    param_id=4,
                    value=7701962,
                    is_numeric=False,
                ),
            ]
        )
        cls.parametrizator.formalizer.on_request(hid=13, query='xiaomi').respond(
            formalized_params=[
                FormalizedParam(
                    param_id=7893318,
                    value=7701962,
                    is_numeric=False,
                ),
            ]
        )
        cls.parametrizator.formalizer.on_request(hid=14, query='iphone').respond(
            formalized_params=[
                FormalizedParam(
                    param_id=5,
                    value=7701962,
                    is_numeric=False,
                ),
            ]
        )
        cls.parametrizator.formalizer.on_request(
            hid=15, query='iphone from old handler', use_new_handler=False
        ).respond(
            formalized_params=[
                FormalizedParam(
                    param_id=6,
                    value=7701962,
                    is_numeric=False,
                ),
            ]
        )
        # fashion категория
        cls.parametrizator.formalizer.on_request(hid=16, query='iphone from new handler', use_new_handler=True).respond(
            formalized_params=[
                FormalizedParam(
                    param_id=7,
                    value=7701962,
                    is_numeric=False,
                ),
            ]
        )
        # не fashion категория (поведём на /fashion обработчик)
        cls.parametrizator.formalizer.on_request(
            hid=17, query='iphone from new handler', use_new_handler=False
        ).respond(
            formalized_params=[
                FormalizedParam(
                    param_id=7,
                    value=7701962,
                    is_numeric=False,
                ),
            ]
        )

    def test_format_json(self):
        """Проверяем, что ответ для json-а приходит в нужно формате при proto и json запросах"""
        expected_response = {"gl_param_values": [{"param": {"param_id": 4, "param_type": 2}}]}
        request = Request(query="black tea", hid=12)

        # передаём json
        response = self.parametrizator.request_json(f'formalize?reqid=reqid&body_json={request.json()}')
        self.assertFragmentIn(response, expected_response)

        # передаём proto
        response = self.parametrizator.request_json('formalize?reqid=reqid', method="POST", body=request.body())
        self.assertFragmentIn(response, expected_response)

    def test_format_proto(self):
        """Проверяем, что ответ для protobuf-а приходит в нужном формате"""
        expected_response_message = ParametrizatorResponse(
            gl_param_values=[
                GlParamValue(
                    param=GlParam(param_id=4, name="", param_type=2),
                    values=[
                        GlValue(
                            value_id=0,
                            value_num=0,
                            name="",
                            value_index_range=Extension(begin=0, end=0),
                            param_index_range=Extension(begin=0, end=0),
                            unit_index_range=Extension(begin=0, end=0),
                        )
                    ],
                )
            ]
        )
        expected_response = expected_response_message.SerializeToString()
        request = Request(query="black tea", hid=12)

        # передаём proto
        response = self.parametrizator.request_protobuf('formalize?reqid=reqid', method="POST", body=request.body())
        self.assertEquals(response.body, expected_response)

    def test_black_list(self):
        """Проверяем, что происходить фильтрация по black list"""
        expected_response = {
            "logic_trace": [
                Regex(r".*Formalized param \(param_id=7893318, value_id=7701962\) was skipped: filtered by blacklist")
            ]
        }
        request = Request(query="xiaomi", hid=13)

        # передаём json
        response = self.parametrizator.request_json(
            f'formalize?reqid=reqid&body_json={request.json()}&use_black_list=1&debug'
        )
        self.assertFragmentIn(response, expected_response)

    def test_cacher(self):
        """Проверяем, что запросы в формализатор кешируются"""
        request = Request(query="iphone", hid=14)

        # cache miss
        response = self.parametrizator.request_json(f'formalize?reqid=reqid&body_json={request.json()}&debug')
        self.assertFragmentIn(
            response,
            {
                "logic_trace": [
                    Regex(r"\[ME\].* Get\(\): Local cache miss for formalizator, key.*"),
                    Regex(r"\[ME\].*, Set\(\): Saved to local cache for formalizator.*"),
                ]
            },
        )

        # cache hit
        response = self.parametrizator.request_json(f'formalize?reqid=reqid&body_json={request.json()}&debug')
        self.assertFragmentIn(
            response,
            {
                "logic_trace": [
                    Regex(r"\[ME\].* Get\(\): Local cache hit for formalizator, key.*"),
                ]
            },
        )

    def test_use_different_formalizer_handelrs(self):
        """Проверяем, что могут использоваться 2 обработчика для запросов в формализатор.
        Ручку /FormalizeForSearch используем только для fashion"""
        # первая ручка
        request = Request(query="iphone from old handler", hid=15)
        expected_response = {
            "gl_param_values": [{"param": {"param_id": 6, "param_type": 2}}],
            "logic_trace": [
                Regex(r".*Get\(\): Local cache miss for formalizator, key http://[^\:]+:[0-9]+/formalize.*")
            ],
        }
        response = self.parametrizator.request_json(
            f'formalize?reqid=reqid&body_json={request.json()}&rearr-factors=market_use_formalizer_for_search=0&debug'
        )
        self.assertFragmentIn(response, expected_response)

        # вторая ручка (fashion категория)
        request = Request(query="iphone from new handler", hid=16)
        expected_response = {
            "gl_param_values": [{"param": {"param_id": 7, "param_type": 2}}],
            "logic_trace": [
                Regex(r".*Get\(\): Local cache miss for formalizator, key http://[^\:]+:[0-9]+/FormalizeForSearch.*")
            ],
        }
        response = self.parametrizator.request_json(
            f'formalize?reqid=reqid&body_json={request.json()}&rearr-factors=market_use_formalizer_for_search=1&debug'
        )
        self.assertFragmentIn(response, expected_response)

        # вторая ручка (не fashion категория)
        request = Request(query="iphone from new handler", hid=17)
        expected_response = {
            "gl_param_values": [{"param": {"param_id": 7, "param_type": 2}}],
            "logic_trace": [
                Regex(r".*Get\(\): Local cache miss for formalizator, key http://[^\:]+:[0-9]+/formalize.*")
            ],
        }
        response = self.parametrizator.request_json(
            f'formalize?reqid=reqid&body_json={request.json()}&rearr-factors=market_use_formalizer_for_search=1&debug'
        )
        self.assertFragmentIn(response, expected_response)

    def test_request_id_pass_through(self):
        """Проверяем что отправляем requestId в сторонние сервисы"""
        ""
        request = Request(query="black tea", hid=12)
        response = self.parametrizator.request_json(f'formalize?reqid=reqid&body_json={request.json()}&debug')
        self.assertFragmentIn(
            response,
            {
                "logic_trace": [
                    Regex(r".*requestId: [0-9]+/[0-9A-Z]+"),
                ]
            },
        )


if __name__ == '__main__':
    import os

    testcase_name = os.path.splitext(os.path.basename(os.path.realpath(__file__)))[0]
    env.main(testcase_name)
