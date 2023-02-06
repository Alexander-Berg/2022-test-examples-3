from datetime import datetime, timezone

import pytest
from db import ExampleJSONB, ExampleJSONBEntity


class TestJSONBEntity:
    @pytest.mark.asyncio
    async def test_maps_and_dumps(self, storage):
        entity = ExampleJSONB(
            id=1,
            data=ExampleJSONBEntity(
                a=1,
                b=datetime(2020, 1, 1, 0, 0, 0, tzinfo=timezone.utc),
            )
        )
        created = await storage.example_jsonb.create(entity)

        assert created.data == entity.data

    @pytest.mark.asyncio
    async def test_does_not_fail_on_unknown_fields(self, storage, db_conn):
        await db_conn.execute(
            """
            insert into sendr_qtools.example_jsonb (id, data)
            values
            (555, '{"a": 1, "b": "2021-01-01T00:00:00+00:00", "c": 5}');
            """
        )

        entity = await storage.example_jsonb.get(555)

        assert entity.data == ExampleJSONBEntity(
            a=1,
            b=datetime(2021, 1, 1, 0, 0, 0, tzinfo=timezone.utc),
        )

    @pytest.mark.asyncio
    async def test_does_not_corrupt_unknown_fields(self, storage, db_conn):
        await db_conn.execute(
            """
            insert into sendr_qtools.example_jsonb (id, data)
            values
            (555, '{"a": 1, "b": "2021-01-01T00:00:00+00:00", "c": 5}');
            """
        )

        entity = await storage.example_jsonb.get(555)
        entity.data.a = 2
        await storage.example_jsonb.save(entity)

        [raw_entity] = [
            row
            async for row in db_conn.execute('select * from sendr_qtools.example_jsonb where id = 555')
        ]

        assert raw_entity['data'] == {"a": 2, "b": "2021-01-01T00:00:00+00:00", "c": 5}

    @pytest.mark.asyncio
    async def test_from_dataclass_preserves_unknown_fields(self, storage, db_conn):
        await db_conn.execute(
            """
            insert into sendr_qtools.example_jsonb (id, data)
            values
            (555, '{"a": 1, "b": "2021-01-01T00:00:00+00:00", "c": 5}');
            """
        )

        entity = await storage.example_jsonb.get(555)
        entity.data = ExampleJSONBEntity.from_dataclass(entity.data)
        await storage.example_jsonb.save(entity)

        [raw_entity] = [
            row
            async for row in db_conn.execute('select * from sendr_qtools.example_jsonb where id = 555')
        ]

        assert raw_entity['data'] == {"a": 1, "b": "2021-01-01T00:00:00+00:00", "c": 5}
