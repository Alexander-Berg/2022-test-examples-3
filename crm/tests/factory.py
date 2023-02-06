from dataclasses import dataclass
from datetime import datetime, timezone
from decimal import Decimal
from typing import Optional

import pytest
from asyncpg import Connection

from smb.common.testing_utils import dt


@dataclass
class Factory:
    con: Connection

    async def create_employee_certificate(
        self,
        id_: int,
        external_id: str = "employee_certificate_id",
        agency_id: int = 22,
        employee_email: str = "alpaca@yandex.ru",
        employee_name: Optional[str] = "Альпак Альпакыч",
        project: str = "Большой проект",
        start_time: Optional[datetime] = None,
        expiration_time: Optional[datetime] = None,
        created_at: Optional[datetime] = None,
    ) -> dict:
        if start_time is None:
            start_time = dt("2020-06-28 18:00:00")
        if expiration_time is None:
            expiration_time = dt("2020-07-28 18:00:00")
        if not created_at:
            created_at = datetime.now(tz=timezone.utc)

        row = await self.con.fetchrow(
            """
            INSERT INTO employee_certificates (
                id,
                external_id,
                agency_id,
                employee_email,
                employee_name,
                project,
                start_time,
                expiration_time,
                created_at
            )
            VALUES (
                $1, $2, $3, $4, $5, $6, $7, $8, $9
            )
            RETURNING *
            """,
            id_,
            external_id,
            agency_id,
            employee_email,
            employee_name,
            project,
            start_time,
            expiration_time,
            created_at,
        )

        return dict(row)

    async def list_employee_certificates(self) -> list[dict]:
        rows = await self.con.fetch(
            """
            SELECT * FROM employee_certificates ORDER BY id
            """,
        )

        return [dict(row) for row in rows]

    async def create_agency_certificate(
        self,
        id_: int,
        external_id: str = "agency_certificate_id",
        agency_id: int = 22,
        project: str = "some_project",
        start_time: Optional[datetime] = None,
        expiration_time: Optional[datetime] = None,
        created_at: Optional[datetime] = None,
    ) -> dict:
        if start_time is None:
            start_time = dt("2020-06-28 18:00:00")
        if expiration_time is None:
            expiration_time = dt("2020-07-28 18:00:00")
        if not created_at:
            created_at = datetime.now(tz=timezone.utc)

        row = await self.con.fetchrow(
            """
            INSERT INTO agency_certificates (
                id,
                external_id,
                agency_id,
                project,
                start_time,
                expiration_time,
                created_at
            )
            VALUES (
                $1, $2, $3, $4, $5, $6, $7
            )
            RETURNING *
            """,
            id_,
            external_id,
            agency_id,
            project,
            start_time,
            expiration_time,
            created_at,
        )

        return dict(row)

    async def list_agency_certificates(self) -> list[dict]:
        rows = await self.con.fetch(
            """
            SELECT * FROM agency_certificates ORDER BY id
            """,
        )

        return [dict(row) for row in rows]

    async def create_certificate_condition(
        self,
        agency_id: int,
        name: str = "Договор с Яндексом",
        value: str = "присутствует",
        threshold: str = "-",
        is_met: bool = True,
    ) -> dict:
        row = await self.con.fetchrow(
            """
            INSERT INTO agency_certificates_direct_conditions (
                agency_id,
                name,
                value,
                threshold,
                is_met
            )
            VALUES ($1, $2, $3, $4, $5)
            RETURNING *
            """,
            agency_id,
            name,
            value,
            threshold,
            is_met,
        )

        return dict(row)

    async def create_direct_kpi(
        self,
        agency_id: int,
        name: str = "Средний максимальный простой",
        value: Decimal = Decimal("1.4"),  # noqa: B008
        max_value: Decimal = Decimal("2.0"),  # noqa: B008
        group: str = "Поиск (настройки показа)",
    ):
        row = await self.con.fetchrow(
            """
            INSERT INTO agency_certificates_direct_kpi (
                agency_id,
                name,
                value,
                max_value,
                group_name
            )
            VALUES ($1, $2, $3, $4, $5)
            RETURNING *
            """,
            agency_id,
            name,
            value,
            max_value,
            group,
        )

        return dict(row)

    async def create_direct_bonus_point(
        self,
        agency_id: int,
        name: str = "Рекламные кейсы с Яндексом за полгода",
        value: str = "4",
        threshold: str = "2",
        score: Decimal = Decimal("1.5"),  # noqa: B008
        is_met: bool = True,
    ):
        row = await self.con.fetchrow(
            """
            INSERT INTO agency_certificates_direct_bonus_scores (
                agency_id,
                name,
                value,
                threshold,
                score,
                is_met
            )
            VALUES ($1, $2, $3, $4, $5, $6)
            RETURNING *
            """,
            agency_id,
            name,
            value,
            threshold,
            score,
            is_met,
        )

        return dict(row)

    async def create_prolongation_score(
        self,
        agency_id: int = 22,
        project: str = "some_project",
        current_score: Decimal = Decimal("2.5"),  # noqa: B008
        target_score: Decimal = Decimal("5.0"),  # noqa: B008
        score_group: str = "general",
    ) -> dict:
        sql = """
            INSERT INTO agency_certificates_prolongation_score (
                agency_id, project, current_score, target_score, score_group
            )
            VALUES ($1, $2, $3, $4, $5)
            RETURNING *
        """

        row = await self.con.fetchrow(
            sql,
            agency_id,
            project,
            current_score,
            target_score,
            score_group,
        )

        return dict(row)

    async def list_direct_bonus_points(self) -> list[dict]:
        rows = await self.con.fetch(
            """
            SELECT * FROM agency_certificates_direct_bonus_scores ORDER BY id
            """,
        )

        return [dict(row) for row in rows]

    async def list_direct_kpis(self) -> list[dict]:
        rows = await self.con.fetch(
            """
            SELECT * FROM agency_certificates_direct_kpi ORDER BY id
            """,
        )

        return [dict(row) for row in rows]

    async def list_direct_conditions(self) -> list[dict]:
        rows = await self.con.fetch(
            """
            SELECT * FROM agency_certificates_direct_conditions ORDER BY id
            """,
        )

        return [dict(row) for row in rows]

    async def list_prolongation_scores(self) -> list[dict]:
        rows = await self.con.fetch(
            """
            SELECT * FROM agency_certificates_prolongation_score ORDER BY id
            """,
        )

        return [dict(row) for row in rows]


@pytest.fixture
def factory(con):
    return Factory(con)
