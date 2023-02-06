import pytest

from hamcrest import assert_that, contains, has_entries, has_properties

from alice.megamind.protos.scenarios.request_pb2 import TScenarioRunRequest
from alice.megamind.protos.scenarios.response_pb2 import TScenarioRunResponse


@pytest.mark.asyncio
async def test_requests_auth(app):
    proto = TScenarioRunRequest()
    response = await app.post(
        '/megamind/run',
        headers={
            'Content-Type': 'application/protobuf',
            'X-Ya-Service-Ticket': 'dbg',
        },
        data=proto.SerializeToString(),
    )
    assert response.status == 200
    response_data = await response.read()
    response_proto = TScenarioRunResponse()
    response_proto.ParseFromString(response_data)
    assert_that(
        response_proto,
        has_properties({
            'ResponseBody': has_properties({
                'Layout': has_properties({
                    'Cards': contains(has_properties({
                        'TextWithButtons': has_properties({
                            'Text': 'Авторизуйтесь, пожалуйста, а затем снова повторите свой запрос.',
                            'Buttons': contains(has_properties({
                                'Title': 'Авторизоваться',
                                'ActionId': 'button_0',
                            })),
                        }),
                    })),
                }),
                'FrameActions': has_entries({
                    'button_0': has_properties({
                        'Directives': has_properties({
                            'List': contains(has_properties({
                                'OpenUriDirective': has_properties({
                                    'Uri': 'yandex-auth://',
                                }),
                            })),
                        }),
                    }),
                }),
            }),
        }),
    )
