import pytest

from asgiref.sync import sync_to_async
from datetime import date, datetime, timezone
from dateutil.relativedelta import relativedelta
from decimal import Decimal
from unittest.mock import Mock

from crm.agency_cabinet.common.consts.reward import RewardsTypes, reward_to_duration, get_end_of_current_fin_year
from crm.agency_cabinet.common.consts.service import Services, name_to_service_id, service_id_to_name
from crm.agency_cabinet.common.yt.base import ConstantExtractor, MethodExtractor

from crm.agency_cabinet.rewards.server.config.clients import YT_CONFIG, YQL_CONFIG
from crm.agency_cabinet.rewards.server.src.celery.tasks.rewards.load_predicted_rewards.base import (
    PredictRewardLoader, PredictServiceRewardLoader
)
from crm.agency_cabinet.rewards.server.src.db import models

REWARDS_TEST_TABLE_PATH = 'REWARDS_TEST_TABLE_PATH'
SERVICE_REWARDS_TEST_TABLE_PATH = 'SERVICE_REWARDS_TEST_TABLE_PATH'


def __get_all_ids(services: str):
    return ', '.join(map(str, name_to_service_id(services)))


@pytest.mark.parametrize(
    'reward_type',
    (reward_type for reward_type in RewardsTypes)
)
@pytest.mark.parametrize(
    'service',
    (service.value for service in Services if service not in Services.get_exception_services_list())
)
@pytest.mark.usefixtures('contract')
async def test_predicted_rewards_loader(contract_id, contract, reward_type, service):
    discount_type = __get_all_ids(service)

    class PredictRewardLoaderMock(PredictRewardLoader):
        _retrieve_table_updated_at = Mock(return_value=datetime.now(tz=timezone.utc))

        def _read_table(self, **_kwargs):
            self._table = [
                (
                    contract_id,
                    (date(2021, 9, 1) + relativedelta(months=i)).isoformat(),
                    (i + 1) * 100_000,
                ) for i in range(0, 12, reward_to_duration(self.reward_type))
            ]
            self._columns = ('contract_id', 'period_from', 'payment')

    class PredictServiceRewardLoaderMock(PredictServiceRewardLoader):
        _retrieve_table_updated_at = Mock(return_value=datetime.now(tz=timezone.utc))

        def _read_table(self, **_kwargs):
            self._table = [
                (
                    contract_id,
                    (date(2021, 9, 1) + relativedelta(months=i)).isoformat(),
                    (i + 1) * 100_000,
                    (i + 1) * 235_000,
                    service,
                    discount_type,
                ) for i in range(0, 12, reward_to_duration(self.reward_type))
            ]
            self._columns = ('contract_id', 'period_from', 'payment', 'revenue', 'service', 'discount_type')

    @sync_to_async
    def load_predicted_rewards():
        predicted_rewards_loader = PredictRewardLoaderMock(
            table_path=REWARDS_TEST_TABLE_PATH,
            model=models.Reward,
            columns_mapper={
                'contract_id': 'contract_id',
                'type': ConstantExtractor(reward_type.value),
                'is_prof': ConstantExtractor(True),
                'payment': 'payment',
                'period_from': MethodExtractor('_extract_period_from'),
                'predict': ConstantExtractor(True),
            },
            default_columns={},
            client_config={
                'cluster': 'hahn',
                'token': YT_CONFIG['TOKEN'],
                'config': {}
            },
            yql_token=YQL_CONFIG['TOKEN'],
            yql_query='',
            reward_type=reward_type,
            force_load=True,
        )
        predicted_rewards_loader.load()

        predicted_service_rewards_loader = PredictServiceRewardLoaderMock(
            table_path=SERVICE_REWARDS_TEST_TABLE_PATH,
            model=models.ServiceReward,
            columns_mapper={
                'reward_id': MethodExtractor('_extract_reward_id'),
                'service': MethodExtractor('_extract_service_name'),
                'discount_type': MethodExtractor('_extract_discount_type'),
                'payment': 'payment',
                'revenue': 'revenue',
            },
            default_columns={},
            client_config={
                'cluster': 'hahn',
                'token': YT_CONFIG['TOKEN'],
                'config': {}
            },
            force_load=predicted_rewards_loader.is_loaded_last_time,
            yql_token=YQL_CONFIG['TOKEN'],
            yql_query='',
            reward_type=reward_type,
        )
        predicted_service_rewards_loader.load()

    await load_predicted_rewards()

    rewards = await models.Reward.select(
        'contract_id',
        'type',
        'got_scan',
        'got_original',
        'is_paid',
        'is_accrued',
        'is_prof',
        'payment',
        'period_from',
        'predict',
    ).where(
        models.Reward.contract_id == contract_id
    ).gino.all()

    expected_rewards = [
        (
            contract_id,
            reward_type.value,
            False,
            False,
            False,
            False,
            True,
            (i + 1) * 100_000,
            datetime(2021, 9, 1, tzinfo=timezone.utc) + relativedelta(months=i),
            True,
        ) for i in range(0, 12, reward_to_duration(reward_type))
    ]

    assert len(rewards) == len(expected_rewards)
    assert rewards == expected_rewards

    reward_ids = [
        row.id
        for row in await models.Reward.select('id').where(models.Reward.contract_id == contract_id).gino.all()
    ]

    service_rewards = await models.ServiceReward.select(
        'service',
        'discount_type',
        'payment',
        'revenue',
    ).where(
        models.ServiceReward.reward_id.in_(reward_ids)
    ).gino.all()

    expected_service_rewards = [
        (
            service_id_to_name(int(discount_type.split(', ')[0])),
            int(discount_type.split(', ')[0]),
            Decimal((i + 1) * 100_000),
            Decimal((i + 1) * 235_000),
        ) for i in range(0, 12, reward_to_duration(reward_type))
    ]

    assert len(service_rewards) == len(expected_service_rewards)
    assert service_rewards == expected_service_rewards

    sync_record = await models.YtSync.query.where(
        models.YtSync.path == f'{REWARDS_TEST_TABLE_PATH}/'
    ).gino.first()
    assert sync_record is not None

    # clean-up
    await models.Reward.delete.where(
        models.Reward.contract_id == contract_id
    ).gino.status()

    await models.YtSync.delete.where(
        models.YtSync.path == f'{REWARDS_TEST_TABLE_PATH}/'
    ).gino.status()


@pytest.mark.parametrize(
    'start_date, end_date',
    [
        (date(2021, 8, 11), date(2022, 3, 1)),
        (date(2022, 1, 30), date(2022, 3, 1)),
        (date(2022, 5, 30), date(2023, 3, 1)),
        (date(2022, 3, 1), date(2023, 3, 1)),
        (date(2022, 12, 1), date(2023, 3, 1)),
        (date(2023, 2, 1), date(2023, 3, 1)),
        (date(2023, 3, 1), date(2024, 3, 1)),
    ]
)
async def test_get_end_period(start_date, end_date):
    assert get_end_of_current_fin_year(start_date) == end_date
