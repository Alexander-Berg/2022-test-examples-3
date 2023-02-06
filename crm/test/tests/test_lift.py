from crm.space.test.components.base import TestBaseSuite
from crm.space.test.schemas.common.grid import GridSchema
from crm.space.test.ora_client import Oracle


class TestBaseLift(TestBaseSuite):
    @staticmethod
    def get_account_data(grid, account_id):
        return next((account["fields"] for account in grid["data"] if account["id"] == account_id), None)


class TestLiftNextPeriod(TestBaseLift):
    path = '/v0/blocks/liftNextPeriod/tableWithFilters'
    method = 'GET'

    def test_get_next_period_grid_parameters(
            self, credential_zomb_crmtest, add_managers_to_lift, add_accounts_to_lift):

        res = self.get_response(**credential_zomb_crmtest)
        assert res.status_code == 200
        grid = GridSchema().loads(res.text)

        assert len(grid["sort"]) == 1
        grid_sort = grid["sort"][0]
        assert grid_sort["id"] == 'avgcost3m'
        assert grid_sort["order"] == 'Desc'
        assert grid_sort["nullsOrderType"] == 'Last'

        assert grid["meta"] == self.get_dict_data('comparison.lift.next_period_meta')
        assert grid["filters"] == self.get_dict_data('comparison.lift.next_period_filters')

    def test_next_period_account_init_db(self, credential_zomb_crmtest, add_managers_to_lift, add_accounts_to_lift):
        ora = Oracle()
        account_id = 73040022
        sql = """UPDATE CRM.LIFT_ACCOUNT SET INIT_SALES_MANAGER_ID=:1, NEW_SALES_MANAGER_ID=:2, LIFT_STATUS=:3,
        UNMANAGED_REASON_ID=:4 WHERE ACCOUNT_ID=:5"""
        values = [(16527, None, 2, None, account_id)]
        ora.nonQueryBatch(sql, values)

        res = self.get_response(**credential_zomb_crmtest)
        assert res.status_code == 200
        grid = GridSchema().loads(res.text)
        assert self.get_account_data(grid, str(account_id)) is not None

    def test_next_period_account_init_db_new_sales_not_null(
            self, credential_zomb_crmtest, add_managers_to_lift, add_accounts_to_lift):
        ora = Oracle()
        account_id = 73040022
        sql = """UPDATE CRM.LIFT_ACCOUNT SET INIT_SALES_MANAGER_ID=:1, NEW_SALES_MANAGER_ID=:2, LIFT_STATUS=:3,
                UNMANAGED_REASON_ID=:4 WHERE ACCOUNT_ID=:5"""
        values = [(16527, 96003, 2, None, account_id)]
        ora.nonQueryBatch(sql, values)

        res = self.get_response(**credential_zomb_crmtest)
        assert res.status_code == 200
        grid = GridSchema().loads(res.text)
        assert self.get_account_data(grid, str(account_id)) is None
