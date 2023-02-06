import datetime
from library.python import resource

from crm.agency_cabinet.ord.server.src.db import db


async def test_query(
    fixture_reports2,
    fixture_contract_procedure,
    fixture_campaign,
    fixture_acts_procedures,
    fixture_organizations,
    fixture_client_rows_has_errors
):

    query = resource.find('query').decode('utf8')
    condition = "WHERE ord.report.period_from >= '2021-03-01' and ord.report.period_from < '2021-04-01'"
    q = db.text(query + condition)
    rows = await db.all(q)

    assert rows == [
        (datetime.datetime(2021, 3, 1, 0, 0, tzinfo=datetime.timezone.utc),
         None, 'contractor inn', 'ip', None, None, None, None, None, None, 'aaaaaa', None, None, None, None, None,
         None, 'some eid', None, None, 1, 'some inn', 'ip', None, None, None, None, None, None, 'some eid', None,
         None, '111', 'test_client_login_1', 'test_client_name_1', 'aaaaaa', None, None, None, None, None, None,
         'some inn', 'ip', None, None, None, None, None, None, None, None, None, None, None, None, None,
         'contractor inn', 'ip', None, None, None, None, None, None, None, None, 'some inn', 'ip', None, None,
         None, None, None, None, None, None, 'campaign eid', '"campaign name"'),
        (datetime.datetime(2021, 3, 1, 0, 0, tzinfo=datetime.timezone.utc),
         None, 'contractor inn', 'ip', None, None, None, None, None, None, 'aaaaaa', None, None, None, None, None,
         None, 'some eid', None, None, 1, 'some inn', 'ip', None, None, None, None, None, None, 'some eid', None,
         None, '222', 'test_client_login_2', 'test_client_name_2', None, None, None, None, None, None, None,
         'some inn', 'ip', None, None, None, None, None, None, 'aaaaaa', None, None, None, None, None, None,
         'contractor inn', 'ip', None, None, None, None, None, None, None, None, 'some inn', 'ip', None, None,
         None, None, None, None, None, None, 'campaign eid', '"campaign name"')
    ]
