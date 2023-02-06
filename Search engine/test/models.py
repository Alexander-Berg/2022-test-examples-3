# coding: utf-8

from search.martylib.yt_utils.models import YtModel

# noinspection PyUnresolvedReferences
from search.martylib.proto.structures.test_pb2 import TestYtModel as martylib__test__TestYtModel  # noqa


class YTestYtModel(YtModel):
    __slots__ = YtModel.__slots__

    DYNAMIC = True

    PROTO_CLASS = martylib__test__TestYtModel
    TABLE_NAME = 'martylib__test__TestYtModel'
    DEFAULT_READ_CLIENT = 'hahn'
    DEFAULT_WRITE_CLIENT = 'markov'

    REPLICAS = [
        'arnold',
        'hahn',
    ]

    SCHEMA = [
        {'name': 'id', 'type': 'string', 'sort_order': 'ascending'},
    ]

    PRIMARY_KEYS = (
        'id',
    )

    def to_row(self, including_default_value_fields=False, primary_keys_only=False):
        result = super(YTestYtModel, self).to_row(
            including_default_value_fields=including_default_value_fields,
            primary_keys_only=primary_keys_only,
        )

        return result

    @classmethod
    def query(
        cls,
        client=None,
        where=None,
        fields=(
            'id',
        ),
        id=None,
        limit=None,
        order_by=None,
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
        table=None,
        **kwargs
    ):
        if where is not None:
            if id is not None:
                raise ValueError('both `where` and `id` used for row filtering')

        if where is None:
            where = set()
            if id is not None:
                where.add(
                    'id = "{}"'.format(id)
                )

            if where:
                where = ' and '.join(where)
            else:
                where = None

        return super(YTestYtModel, cls).query(
            client=client,
            where=where,
            limit=limit,
            order_by=order_by,
            fields=fields,
            timestamp=timestamp,
            input_row_limit=input_row_limit,
            output_row_limit=output_row_limit,
            range_expansion_limit=range_expansion_limit,
            fail_on_incomplete_result=fail_on_incomplete_result,
            verbose_logging=verbose_logging,
            enable_code_cache=enable_code_cache,
            max_subqueries=max_subqueries,
            workload_descriptor=workload_descriptor,
            allow_full_scan=allow_full_scan,
            allow_join_without_index=allow_join_without_index,
            format=format,
            raw=raw,
            execution_pool=execution_pool,
            table=table,
            **kwargs
        )

    @classmethod
    def get(
        cls,
        client=None,
        where=None,
        fields=(
            'id',
        ),
        id=None,
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
    ):
        return super(YTestYtModel, cls).get(
            client=client,
            where=where,
            fields=fields,
            id=id,
            timestamp=timestamp,
            input_row_limit=input_row_limit,
            output_row_limit=output_row_limit,
            range_expansion_limit=range_expansion_limit,
            fail_on_incomplete_result=fail_on_incomplete_result,
            verbose_logging=verbose_logging,
            enable_code_cache=enable_code_cache,
            max_subqueries=max_subqueries,
            workload_descriptor=workload_descriptor,
            allow_full_scan=allow_full_scan,
            allow_join_without_index=allow_join_without_index,
            format=format,
            raw=raw,
            execution_pool=execution_pool,
            **kwargs
        )
