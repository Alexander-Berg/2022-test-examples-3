from datetime import datetime

from crm.agency_cabinet.ord.server.src.db import models

from crm.agency_cabinet.ord.server.src.celery.tasks.load_reports.synchronizer import ReportsSynchronizer
from crm.agency_cabinet.grants.common.structs import ListPartnersResponse, Partner


# TODO: use fixtures
PARTNER_ID = 123
AGENCY_ID = 1
CLIENT_ID = 111
CLIENT_LOGIN = 'login'
CLIENT_NAME = 'name'
ACT_EID = '111'
ACT_ID = 1
ACTS = [
    {
        'id': ACT_ID,
        'eid': ACT_EID,
        'amount': 100,
        'campaign_eid': 111,
        'campaign_name': 'campaign 111',
    },
    {
        'id': ACT_ID,
        'eid': ACT_EID,
        'amount': 200,
        'campaign_eid': 222,
        'campaign_name': 'campaign 222',
    },
    {
        'id': ACT_ID,
        'eid': ACT_EID,
        'amount': 300,
        'campaign_eid': 333,
        'campaign_name': 'campaign 333',
    },
    {
        'id': ACT_ID,
        'eid': ACT_EID,
        'amount': 400,
        'campaign_eid': 444,
        'campaign_name': 'campaign 444',
    },
    {
        'id': ACT_ID,
        'eid': ACT_EID,
        'amount': 500,
        'campaign_eid': 555,
        'campaign_name': 'campaign 555',
    },
]


async def test_report_loads(fixture_report_settings, service_discovery):
    service_discovery.grants.list_partners.return_value = ListPartnersResponse(
        partners=[
            Partner(
                partner_id=PARTNER_ID,
                type='agency',
                external_id=str(AGENCY_ID),
                name='Test'
            ),
        ]
    )

    await ReportsSynchronizer(datetime(2022, 3, 1), sd=service_discovery).process_data(
        [
            (AGENCY_ID, CLIENT_ID, act['amount'], act['id'], act['eid'], CLIENT_LOGIN, CLIENT_NAME, act['campaign_eid'],
             act['campaign_name']) for act in ACTS
        ]
    )

    report = await models.Report.query.gino.first()
    assert report is not None
    assert report.agency_id == PARTNER_ID
    assert report.external_id == str(AGENCY_ID)

    clients = await models.Client.query.where(models.Client.report_id == report.id).gino.all()
    assert len(clients) == 1
    assert clients[0].client_id == str(CLIENT_ID)
    assert clients[0].login == CLIENT_LOGIN
    assert clients[0].name == CLIENT_NAME
    client_id = clients[0].id

    campaigns = await models.Campaign.query.where(models.Campaign.report_id == report.id).order_by(
        models.Campaign.id).gino.all()
    assert len(campaigns) == 5
    for campaign, expected in zip(campaigns, ACTS):
        assert campaign.client_id == client_id
        assert campaign.campaign_eid == str(expected['campaign_eid'])
        assert campaign.name == expected['campaign_name']

    acts = await models.Act.query.where(models.Act.report_id == report.id).gino.all()
    assert len(acts) == 1
    assert acts[0].amount == 1500

    client_rows = await models.ClientRow.query.where(models.ClientRow.client_id.in_([i.id for i in clients])).order_by(
        models.ClientRow.id).gino.all()
    assert len(client_rows) == 5
    for client_row, expected in zip(client_rows, ACTS):
        assert client_row.suggested_amount == expected['amount']

    await models.ClientRow.delete.where(models.ClientRow.client_id.in_([i.id for i in clients])).gino.all()
    await models.Act.delete.where(models.Act.report_id == report.id).gino.all()
    await models.Campaign.delete.where(models.Campaign.report_id == report.id).gino.all()
    await models.Client.delete.where(models.Client.report_id == report.id).gino.all()
    await report.delete()
