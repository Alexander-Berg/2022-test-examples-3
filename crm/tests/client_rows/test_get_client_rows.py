from decimal import Decimal

from crm.agency_cabinet.ord.common import consts, structs
from crm.agency_cabinet.ord.proto import request_pb2, campaigns_pb2, client_rows_pb2, organizations_pb2, \
    acts_pb2, contracts_pb2


async def test_get_client_rows_sends_correct_request(client, rmq_rpc_client):
    rmq_rpc_client.send_proto_message.return_value = client_rows_pb2.GetClientRowsOutput(
        result=client_rows_pb2.ClientRowsList(
            size=5,
            rows=[
                client_rows_pb2.ClientRow(
                    id=1,
                    suggested_amount='6000.0',
                    campaign=campaigns_pb2.Campaign(
                        campaign_eid='campaign eid',
                        name='campaign name',
                        id=1,
                        creative_id='1,2,3,4,5'
                    ),
                    ad_distributor_organization=organizations_pb2.Organization(
                        id=41,
                        name='Test org 1',
                        type=0,
                        inn='1234567890'
                    ),
                    ad_distributor_partner_organization=organizations_pb2.Organization(
                        id=42,
                        name='Test org 2',
                        type=0,
                        inn='1234567890'
                    ),
                    partner_client_organization=organizations_pb2.Organization(
                        id=43,
                        name='Test org 3',
                        type=0,
                        inn='1234567890'
                    ),
                    advertiser_contractor_organization=organizations_pb2.Organization(
                        id=44,
                        name='Test org 4',
                        type=0,
                        inn='1234567890'
                    ),
                    advertiser_organization=organizations_pb2.Organization(
                        id=45,
                        name='Test org 5',
                        type=0,
                        inn='1234567890'
                    ),
                    ad_distributor_contract=contracts_pb2.Contract(
                        id=1,
                        contract_eid='123',
                    ),
                    ad_distributor_partner_contract=contracts_pb2.Contract(
                        id=2,
                        contract_eid='234',
                    ),
                    advertiser_contract=contracts_pb2.Contract(
                        id=3,
                        contract_eid='345',
                    ),
                    ad_distributor_act=acts_pb2.Act(
                        act_id=1,
                        act_eid='act1',
                        amount='10',
                        is_vat=True,
                    ),
                    ad_distributor_partner_act=acts_pb2.Act(
                        act_id=2,
                        act_eid='act2',
                        is_vat=False,
                    ),
                )
            ]))

    got = await client.get_client_rows(agency_id=1, report_id=1, client_id=1)

    rmq_rpc_client.send_proto_message.assert_awaited_with(
        queue_name='ord',
        message=request_pb2.RpcRequest(
            get_client_rows=client_rows_pb2.GetClientRowsInput(
                agency_id=1,
                report_id=1,
                client_id=1,
            )
        ),
        response_message_type=client_rows_pb2.GetClientRowsOutput,
    )

    assert got == structs.ClientRowsList(
        size=5,
        rows=[
            structs.ClientRow(
                id=1,
                suggested_amount=Decimal('6000.0'),
                campaign=structs.Campaign(
                    id=1,
                    campaign_eid='campaign eid',
                    name='campaign name',
                    creative_id='1,2,3,4,5',
                ),
                ad_distributor_organization=structs.Organization(
                    id=41,
                    name='Test org 1',
                    type=consts.OrganizationType.ffl,
                    inn='1234567890',
                ),
                ad_distributor_partner_organization=structs.Organization(
                    id=42,
                    name='Test org 2',
                    type=consts.OrganizationType.ffl,
                    inn='1234567890',
                ),
                partner_client_organization=structs.Organization(
                    id=43,
                    name='Test org 3',
                    type=consts.OrganizationType.ffl,
                    inn='1234567890',
                ),
                advertiser_contractor_organization=structs.Organization(
                    id=44,
                    name='Test org 4',
                    type=consts.OrganizationType.ffl,
                    inn='1234567890',
                ),
                advertiser_organization=structs.Organization(
                    id=45,
                    name='Test org 5',
                    type=consts.OrganizationType.ffl,
                    inn='1234567890',
                ),
                ad_distributor_contract=structs.Contract(
                    id=1,
                    contract_eid='123',
                ),
                ad_distributor_partner_contract=structs.Contract(
                    id=2,
                    contract_eid='234',
                ),
                advertiser_contract=structs.Contract(
                    id=3,
                    contract_eid='345',
                ),
                ad_distributor_act=structs.Act(
                    act_id=1,
                    act_eid='act1',
                    amount=Decimal(10),
                    is_vat=True,
                ),
                ad_distributor_partner_act=structs.Act(
                    act_id=2,
                    act_eid='act2',
                    is_vat=False,
                ),
            )
        ]
    )
