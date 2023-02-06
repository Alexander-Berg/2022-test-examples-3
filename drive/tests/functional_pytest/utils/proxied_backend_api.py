from datetime import datetime

import pytest

from drive.backend.api.client import BackendClient


def allure_report(response):
    with pytest.allure.step('{} {}'.format(
        response.request.method,
        response.url
    )):
        pytest.allure.attach(
            'Request',
            '\n\n'.join(
                [
                    'Time: {}'.format(datetime.now().strftime('%d %b %Y %H:%M:%S')),
                ]
                +
                [
                    str(response.request.body)
                ]
            ),
            pytest.allure.attach_type.TEXT,
        )
        pytest.allure.attach(
            f'Response ({response.status_code})',
            response.content,
            pytest.allure.attach_type.TEXT,
        )


class BackendClientAutotests(BackendClient):

    def _request(self, *args, **kwargs):
        return super()._request(*args, **kwargs, response_handler=allure_report)

