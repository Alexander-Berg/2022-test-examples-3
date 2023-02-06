import yenv

from sendr_aiohttp.handler import request_schema, response_schema

from mail.payments.payments.api.handlers.base import BaseHandler, RequestMadeByUIDHandlerMixin
from mail.payments.payments.api.schemas.base import fail_response_schema, success_response_schema
from mail.payments.payments.api.schemas.path import uid_request_schema
from mail.payments.payments.core.actions.testing.delete_user import DeleteUserAction


class DeleteUserHandler(RequestMadeByUIDHandlerMixin, BaseHandler):
    @request_schema(uid_request_schema, location='match_info')
    @response_schema(success_response_schema)
    async def delete(self):
        if yenv.type == 'production':
            return self.make_response({}, fail_response_schema, status=403)

        data = await self.get_data()

        await self.run_action(DeleteUserAction, data)
        return self.make_response({}, success_response_schema)
