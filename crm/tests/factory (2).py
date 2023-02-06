from dataclasses import dataclass
from datetime import datetime, timezone
from decimal import Decimal
from typing import Optional
from crm.agency_cabinet.client_bonuses.common.structs import ClientType

import pytest
from asyncpg import Connection


@dataclass
class Factory:
    con: Connection

    async def list_clients(self) -> list[dict]:
        rows = await self.con.fetch(
            """
            SELECT *
            FROM clients
            ORDER BY id
            """
        )

        return list(map(dict, rows))

    async def create_client(
        self,
        id_: int,
        login: Optional[str] = None,
        agency_id: int = 22,
        is_active: bool = True,
        create_date: datetime = datetime.now(tz=timezone.utc)
    ) -> dict:
        if login is None:
            login = f"login{id_}"

        row = await self.con.fetchrow(
            """
            INSERT INTO clients (id, login, agency_id, is_active, create_date)
            VALUES ($1, $2, $3, $4, $5)
            RETURNING *
            """,
            id_,
            login,
            agency_id,
            is_active,
            create_date,
        )

        return dict(row)

    async def list_clients_programs(self) -> list[dict]:
        rows = await self.con.fetch(
            """
            SELECT *
            FROM clients_programs
            ORDER BY client_id, program_id
            """
        )

        return list(map(dict, rows))

    async def create_client_program(
        self,
        client_id: int,
        program_id: int,
    ) -> dict:
        row = await self.con.fetchrow(
            """
            INSERT INTO clients_programs (client_id, program_id)
            VALUES ($1, $2)
            RETURNING *
            """,
            client_id,
            program_id,
        )

        return dict(row)

    async def list_gained_client_bonuses(self) -> list[dict]:
        rows = await self.con.fetch(
            """
            SELECT *
            FROM gained_client_bonuses
            ORDER BY client_id, gained_at, program_id
            """
        )

        return list(map(dict, rows))

    async def create_gained_client_bonuses(
        self,
        client_id: int,
        gained_at: datetime,
        program_id: int,
        currency: str,
        amount: Decimal = Decimal("1234.56"),  # noqa: B008
    ) -> dict:
        row = await self.con.fetchrow(
            """
            INSERT INTO gained_client_bonuses (
                client_id,
                gained_at,
                program_id,
                amount,
                currency
            )
            VALUES ($1, $2, $3, $4, $5)
            RETURNING *
            """,
            client_id,
            gained_at,
            program_id,
            amount,
            currency
        )

        return dict(row)

    async def list_spent_client_bonuses(self) -> list[dict]:
        rows = await self.con.fetch(
            """
            SELECT *
            FROM spent_client_bonuses
            ORDER BY client_id, spent_at
            """
        )

        return list(map(dict, rows))

    async def create_spent_client_bonuses(
        self,
        client_id: int,
        spent_at: datetime,
        currency: str,
        amount: Decimal = Decimal("1234.56"),  # noqa: B008
    ) -> dict:
        row = await self.con.fetchrow(
            """
            INSERT INTO spent_client_bonuses (client_id, spent_at, amount, currency)
            VALUES ($1, $2, $3, $4)
            RETURNING *
            """,
            client_id,
            spent_at,
            amount,
            currency
        )

        return dict(row)

    async def list_client_bonuses_to_activate(self) -> list[dict]:
        rows = await self.con.fetch(
            """
            SELECT *
            FROM client_bonuses_to_activate
            ORDER BY client_id
            """
        )

        return list(map(dict, rows))

    async def create_client_bonuses_to_activate(
        self, client_id: int, amount: Decimal = Decimal("1234.56")  # noqa: B008
    ) -> dict:
        row = await self.con.fetchrow(
            """
            INSERT INTO client_bonuses_to_activate (client_id, amount)
            VALUES ($1, $2)
            RETURNING *
            """,
            client_id,
            amount,
        )

        return dict(row)

    async def create_report_meta_info(
        self,
        name: str,
        agency_id: int,
        period_from: datetime,
        period_to: datetime,
        client_type: ClientType,
        status: str,
        file_id: int
    ) -> dict:
        row = await self.con.fetchrow(
            """
            INSERT INTO report_meta_info (
                name,
                agency_id,
                period_from,
                period_to,
                client_type,
                status,
                file_id
            )
            VALUES ($1, $2, $3, $4, $5, $6, $7)
            RETURNING *
            """,
            name,
            agency_id,
            period_from,
            period_to,
            client_type.value,
            status,
            file_id
        )

        return dict(row)

    async def create_s3_mds_file(
        self,
        bucket: str,
        name: str,
        display_name: str,
    ) -> dict:
        row = await self.con.fetchrow(
            """
            INSERT INTO s3_mds_file (
                bucket,
                name,
                display_name
            )
            VALUES ($1, $2, $3)
            RETURNING *
            """,
            bucket,
            name,
            display_name
        )

        return dict(row)

    async def create_cashback_program(
        self,
        id: int,
        category_id: int,
        is_general: bool,
        is_enabled: bool,
        name_ru: str,
        name_en: str,
        description_ru: str,
        description_en: str,
    ) -> dict:
        row = await self.con.fetchrow(
            """
            INSERT INTO cashback_programs (
                id,
                category_id,
                is_general,
                is_enabled,
                name_ru,
                name_en,
                description_ru,
                description_en
            )
            VALUES ($1, $2, $3, $4, $5, $6, $7, $8)
            RETURNING *
            """,
            id,
            category_id,
            is_general,
            is_enabled,
            name_ru,
            name_en,
            description_ru,
            description_en,
        )

        return dict(row)

    async def list_cashback_programs(self) -> list[dict]:
        rows = await self.con.fetch(
            """
            SELECT *
            FROM cashback_programs
            ORDER BY id
            """
        )

        return list(map(dict, rows))


@pytest.fixture
def factory(con):
    return Factory(con)
