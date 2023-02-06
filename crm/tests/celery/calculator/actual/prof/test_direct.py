from dataclasses import dataclass
from sqlalchemy import and_

from crm.agency_cabinet.rewards.server.src.db import models
from crm.agency_cabinet.rewards.server.src.celery.tasks.calculator.actual.prof.synchronizer import \
    ProfDirectActualDataSynchronizer


@dataclass
class DirectDataStruct:
    dt: str
    early_payment: float
    predict_m_amt_rsya: float
    predict_q_amt_auto_Q3: float
    predict_q_amt_goal_Q3: float
    predict_q_amt_metrika: float
    predict_q_amt_video_cpc_Q3: float
    predict_q_amt_smart_banner: float
    predict_q_amt_rmp: float
    predict_q_amt_retargeting: float
    predict_q_amt_k50: float
    predict_q_amt_product_gallery: float
    predict_q_amt_search_autotargeting_not_uac: float
    predict_m_uniqd_prof_d_150k: float
    predict_m_amt_prof_d_150k: float
    predict_m_uniqd_prof_c_150k_800k: float
    predict_m_amt_prof_c_150k_800k: float
    predict_m_uniqd_prof_b_800k_5m: float
    predict_m_amt_prof_b_800k_5m: float
    predict_m_uniqd_prof_a_5m: float
    predict_m_amt_prof_a_5m: float


async def test_add_calculator_data_direct(contract):
    await ProfDirectActualDataSynchronizer().process_data(
        [
            (contract.id, [DirectDataStruct('2022-01-01',
                                            15451.06,
                                            14451.06,
                                            1,
                                            2,
                                            3,
                                            4,
                                            5,
                                            6,
                                            7,
                                            8,
                                            9,
                                            10,
                                            11,
                                            12,
                                            13,
                                            14,
                                            15,
                                            16,
                                            17,
                                            18
                                            )]),
        ]
    )

    data = await models.CalculatorData.query.where(
        and_(
            models.CalculatorData.contract_id == contract.id,
            models.CalculatorData.service == 'direct',
            models.CalculatorData.version == '2022'
        )
    ).gino.first()

    assert data is not None
    await data.delete()

    assert data.data == {'months': [{'grades': [{'domains_count': 11,
                         'grade_id': 'D',
                                                 'revenue_average': 1.0909090909090908},
                                                {'domains_count': 13,
                                                 'grade_id': 'C',
                                                 'revenue_average': 150001.07692307694},
                                                {'domains_count': 15,
                                                 'grade_id': 'B',
                                                 'revenue_average': 800001.0666666667},
                                                {'domains_count': 17,
                                                 'grade_id': 'A',
                                                 'revenue_average': 5000001.05882353}],
                                     'indexes': [{'index_id': 'early_payment', 'revenue': 15451.06},
                                                 {'index_id': 'rsya', 'revenue': 14451.06},
                                                 {'index_id': 'conversion_autostrategy',
                                                  'revenue': 1.0},
                                                 {'index_id': 'key_goals', 'revenue': 2.0},
                                                 {'index_id': 'metrica', 'revenue': 3.0},
                                                 {'index_id': 'video_cpc', 'revenue': 4.0},
                                                 {'index_id': 'smart_banners', 'revenue': 5.0},
                                                 {'index_id': 'rmp', 'revenue': 6.0},
                                                 {'index_id': 'retargeting', 'revenue': 7.0},
                                                 {'index_id': 'k50', 'revenue': 8.0},
                                                 {'index_id': 'product_gallery', 'revenue': 9.0},
                                                 {'index_id': 'search_autotargeting', 'revenue': 10.0}],
                                     'period_from': '2022-01-01T00:00:00',
                                     'predict': False}]}
