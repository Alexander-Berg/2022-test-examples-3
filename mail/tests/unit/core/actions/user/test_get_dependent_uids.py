import pytest

from mail.beagle.beagle.core.actions.user.get_dependent_uids import GetDependentUIDsUserAction


class TestGetDependentUIDsUserAction:
    @pytest.fixture
    def returned_func(self, user):
        async def _inner(org_id=user.org_id, uid=user.uid):
            return await GetDependentUIDsUserAction(org_id=org_id, uid=uid).run()

        return _inner

    @pytest.mark.asyncio
    async def test_nonexistent_user(self, user, returned_func):
        assert await returned_func(org_id=user.org_id, uid=user.uid + 1) == set()

    def test_no_subscriptions(self, returned):
        assert returned == set()

    def test_user_subscription(self, mail_list, user_subscription, returned):
        assert returned == {mail_list.uid}

    def test_unit_subscription(self, mail_list, unit_user, unit_subscription, returned):
        assert returned == {mail_list.uid}

    class TestMultipleMailLists:
        @pytest.fixture
        def setup_mail_lists(storge,
                             org,
                             create_mail_list,
                             create_user_subscription,
                             create_unit_subscription,
                             ):
            async def _inner(user, unit):
                mail_lists = [await create_mail_list(org.org_id) for _ in range(5)]

                for mail_list in mail_lists[:3]:
                    await create_user_subscription(
                        org_id=org.org_id,
                        mail_list_id=mail_list.mail_list_id,
                        uid=user.uid,
                    )
                for mail_list in mail_lists[3:]:
                    await create_unit_subscription(
                        org_id=org.org_id,
                        mail_list_id=mail_list.mail_list_id,
                        unit_id=unit.unit_id,
                    )

                return mail_lists

            return _inner

        @pytest.fixture
        async def affected_mail_lists(self, user, unit, unit_user, setup_mail_lists):
            return await setup_mail_lists(user, unit)

        @pytest.fixture
        async def not_affected_mail_lists(self, org, create_user, create_unit, create_unit_user, setup_mail_lists):
            new_user = await create_user(org.org_id)
            new_unit = await create_unit(org.org_id)
            await create_unit_user(unit=new_unit, user=new_user)
            return await setup_mail_lists(user=new_user, unit=new_unit)

        def test_returned(self, affected_mail_lists, not_affected_mail_lists, returned):
            assert returned == {mail_list.uid for mail_list in affected_mail_lists}
