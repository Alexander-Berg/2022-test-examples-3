from crm.agency_cabinet.ord.server.src.db import models


async def assert_report_data(client_rows,
                             expected_partner_acts,
                             expected_partner_contracts,
                             expected_partner_client_orgs,
                             expected_advertiser_contracts,
                             expected_advertiser_orgs,
                             expected_advertiser_contractor_orgs):

    data = zip(
        client_rows,
        expected_partner_acts,
        expected_partner_contracts,
        expected_partner_client_orgs,
        expected_advertiser_contracts,
        expected_advertiser_orgs,
        expected_advertiser_contractor_orgs
    )

    for row, expected_partner_act, expected_partner_contract, expected_partner_client_org, \
            expected_advertiser_contract, expected_advertiser_org, expected_advertiser_contractor_org in data:

        partner_act = await models.Act.query.where(models.Act.id == row.partner_act_id).gino.first()
        assert partner_act.act_eid == expected_partner_act[0]
        assert partner_act.amount == expected_partner_act[1]
        assert partner_act.is_vat == expected_partner_act[2]

        # Договор между агентством и его контрагентом
        partner_contract = await models.Contract.query.where(models.Contract.id == row.partner_contract_id).gino.first()
        assert partner_contract.contract_eid == expected_partner_contract[0]
        assert partner_contract.type == expected_partner_contract[1]
        assert partner_contract.action_type == expected_partner_contract[2]
        assert partner_contract.subject_type == expected_partner_contract[3]
        assert partner_contract.date == expected_partner_contract[4]
        assert partner_contract.amount == expected_partner_contract[5]
        assert partner_contract.is_vat == expected_partner_contract[6]

        # Контрагент агентства
        partner_client_org = await models.Organization.query.where(
            models.Organization.id == partner_contract.client_id
        ).gino.first()
        assert partner_client_org.inn == expected_partner_client_org[0]
        assert partner_client_org.type == expected_partner_client_org[1]
        assert partner_client_org.name == expected_partner_client_org[2]

        # Договор между конечным рекламодателем и его исполнителем
        advertiser_contract = await models.Contract.query.where(
            models.Contract.id == row.advertiser_contract_id
        ).gino.first()
        assert advertiser_contract.contract_eid == expected_advertiser_contract[0]
        assert advertiser_contract.type == expected_advertiser_contract[1]
        assert advertiser_contract.action_type == expected_advertiser_contract[2]
        assert advertiser_contract.subject_type == expected_advertiser_contract[3]
        assert advertiser_contract.date == expected_advertiser_contract[4]
        assert advertiser_contract.amount == expected_advertiser_contract[5]
        assert advertiser_contract.is_vat == expected_advertiser_contract[6]

        # Конечный рекламодатель
        advertiser_org = await models.Organization.query.where(
            models.Organization.id == advertiser_contract.client_id
        ).gino.first()
        assert advertiser_org.inn == expected_advertiser_org[0]
        assert advertiser_org.type == expected_advertiser_org[1]
        assert advertiser_org.name == expected_advertiser_org[2]

        # Контрагент конечного рекламодателя
        advertiser_contractor_org = await models.Organization.query.where(
            models.Organization.id == advertiser_contract.contractor_id
        ).gino.first()
        assert advertiser_contractor_org.inn == expected_advertiser_contractor_org[0]
        assert advertiser_contractor_org.type == expected_advertiser_contractor_org[1]
        assert advertiser_contractor_org.name == expected_advertiser_contractor_org[2]
