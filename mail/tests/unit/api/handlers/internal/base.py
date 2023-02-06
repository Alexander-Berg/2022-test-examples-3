import pytest


class BaseInternalHandlerTvmTest:
    class TestUserNotAuthorizedException:
        @pytest.fixture(autouse=True)
        def action(self):
            pass

        @pytest.fixture(autouse=True)
        def run_action_calls(self):
            pass

        @pytest.fixture(autouse=True)
        def mock_tvm_src(self, mocker, service_client):
            mocker.patch(
                'mail.payments.payments.api.handlers.internal.base.BaseInternalHandler.tvm_src',
                service_client.tvm_id + 1
            )

        @pytest.mark.asyncio
        async def test_user_not_authorized(self, response):
            assert response.status == 403
