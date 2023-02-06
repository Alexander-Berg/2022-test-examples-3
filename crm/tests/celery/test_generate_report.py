import pytest
import xlrd
from decimal import Decimal

from smb.common.testing_utils import dt

from crm.agency_cabinet.common.server.common.config import MdsConfig
from crm.agency_cabinet.client_bonuses.common.structs import ClientType
from crm.agency_cabinet.client_bonuses.server.lib.celery.tasks.generate_report import ReportGenerator

pytestmark = [
    pytest.mark.asyncio
]


@pytest.fixture
async def client(factory):
    client = await factory.create_client(id_=321)

    return client


@pytest.fixture
async def create_spends(client, factory):
    await factory.create_spent_client_bonuses(
        client_id=client['id'],
        spent_at=dt("2020-06-02 18:00:00"),
        amount=Decimal("500"),
        currency="RUR",
    )
    await factory.create_spent_client_bonuses(
        client_id=client['id'],
        spent_at=dt("2020-07-03 18:00:00"),
        amount=Decimal("600"),
        currency="RUR",
    )


@pytest.fixture
async def create_gains(client, factory):
    await factory.create_cashback_program(
        id=1,
        category_id=1,
        is_general=True,
        is_enabled=True,
        name_ru="Программа 1",
        name_en="Program 1",
        description_ru="Программа кэшбека #1",
        description_en="Cashback program #1",
    )
    await factory.create_cashback_program(
        id=2,
        category_id=2,
        is_general=True,
        is_enabled=True,
        name_ru="Программа 2",
        name_en="Program 2",
        description_ru="Программа кэшбека #2",
        description_en="Cashback program #2",
    )
    await factory.create_gained_client_bonuses(
        client_id=client['id'],
        gained_at=dt("2020-04-01 18:00:00"),
        program_id=1,
        amount=Decimal("100"),
        currency="RUR",
    )
    await factory.create_gained_client_bonuses(
        client_id=client['id'],
        gained_at=dt("2020-05-01 18:00:00"),
        program_id=1,
        amount=Decimal("100.20"),
        currency="RUR",
    )
    await factory.create_gained_client_bonuses(
        client_id=client['id'],
        gained_at=dt("2020-05-01 18:00:00"),
        program_id=2,
        amount=Decimal("10.20"),
        currency="RUR",
    )
    await factory.create_gained_client_bonuses(
        client_id=client['id'],
        gained_at=dt("2020-06-02 18:00:00"),
        program_id=2,
        amount=Decimal("20.20"),
        currency="RUR",
    )


@pytest.fixture
async def report_id(client, factory):
    report = await factory.create_report_meta_info(
        name='test',
        agency_id=22,
        period_from=dt("2020-03-02 18:00:00"),
        period_to=dt("2020-08-02 18:00:00"),
        client_type=ClientType.ALL,
        status='requested',
        file_id=None
    )

    return report["id"]


@pytest.fixture()
def mds_cfg():
    environ_dict = {
        'MDS_ENDPOINT_URL': None,
        'MDS_ACCESS_KEY_ID': None,
        'MDS_SECRET_ACCESS_KEY': None
    }
    return MdsConfig.from_environ(environ_dict)


@pytest.fixture
def generator(db, mds_cfg, report_id):
    return ReportGenerator(db, mds_cfg, report_id)


@pytest.mark.skip('Fix error: xlrd.biffh.XLRDError: Excel xlsx file; not supported')
async def test_generate_report(client, generator, create_gains, create_spends):
    report, report_name = await generator._make_report()
    book = xlrd.open_workbook(report.name)
    assert book.sheet_names() == ['Всего', 'Апрель 2020', 'Май 2020', 'Июнь 2020', 'Июль 2020']

    total_sheet = book.sheet_by_index(0)
    assert [cell.value for cell in total_sheet.row(0)] == ['client id', 'client login', 'Накоплено бонусов (₽, с НДС)', 'Потрачено бонусов (₽, с НДС)']
    assert [cell.value for cell in total_sheet.row(1)] == [client['id'], client['login'], 230.6, 1100.0]
