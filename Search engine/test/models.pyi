# coding: utf-8

from search.martylib.yt_utils.models import YtModel

# noinspection PyUnresolvedReferences
from search.martylib.proto.structures.test_pb2 import TestYtModel as martylib__test__TestYtModel  # noqa
import typing


class YTestYtModel(YtModel):
    proto: martylib__test__TestYtModel

    PROTO_CLASS: typing.Type[martylib__test__TestYtModel] = martylib__test__TestYtModel
    TABLE_NAME: str = 'martylib__test__TestYtModel'

    SCHEMA: typing.List[typing.Dict[str, str]] = [
        {'name': 'id', 'type': 'string', 'sort_order': 'ascending'},
    ]

    PRIMARY_KEYS: typing.Iterable[str] = (
        'id',
    )

    @classmethod
    def query(
        cls,
        client=None,
        where: typing.Optional[str] = None,
        limit: typing.Optional[int] = None,
        fields: typing.Iterable[str] = (
            'id',
        ),
        id: typing.Optional[str] = None,
        timestamp=None,
        input_row_limit=None,
        output_row_limit=None,
        range_expansion_limit=None,
        fail_on_incomplete_result=None,
        verbose_logging=None,
        enable_code_cache=None,
        max_subqueries=None,
        workload_descriptor=None,
        allow_full_scan=None,
        allow_join_without_index=None,
        format=None,
        raw=None,
        execution_pool=None,
        **kwargs
    ) -> typing.List[YTestYtModel]:
        ...

    @classmethod
    def get(
        cls,
        client=None,
        where: typing.Optional[str] = None,
        fields: typing.Iterable[str] = (
            'id',
        ),
        id: typing.Optional[str] = None,
        timestamp=None,
        input_row_limit=None,
        output_row_limit=None,
        range_expansion_limit=None,
        fail_on_incomplete_result=None,
        verbose_logging=None,
        enable_code_cache=None,
        max_subqueries=None,
        workload_descriptor=None,
        allow_full_scan=None,
        allow_join_without_index=None,
        format=None,
        raw=None,
        execution_pool=None,
        **kwargs
    ) -> YTestYtModel:
        ...
