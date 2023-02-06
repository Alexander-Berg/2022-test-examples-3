from search.martylib.abc import AbcClientMock as _AbcClientMock


class AbcClientMock(_AbcClientMock):

    n_mock_users = 10

    def get_service_members(self, service_slug, role=None, role_scope=None, fields=None, with_descendants=False):
        return [
            {
                'person': {
                    'login': '{service_slug}-user{suffix}'.format(
                        service_slug=service_slug,
                        suffix=('-{}'.format(idx) if idx else ''),
                    )
                }
            }
            for idx in range(self.n_mock_users)
        ]
