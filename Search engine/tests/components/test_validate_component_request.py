from search.martylib.test_utils import TestCase
from search.mon.warden.proto.structures import component_pb2
from search.mon.warden.src.services.reducers import component as r_component


class TestWardenValidateComponentRequests(TestCase):
    maxDiff = None

    def test_validation_root_requests(self):
        test_cases = [
            {
                'name': 'All good',
                'request': component_pb2.CreateComponentRequest(
                    component=component_pb2.Component(
                        slug='name',
                        name='name',
                        loading_dashboard_url='http://www.example.com/index?search=src',
                        abc_service_slug='abs'
                    ),
                ),
                'expected': ''
            },
            {
                'name': 'Empty component name',
                'request': component_pb2.CreateComponentRequest(
                    component=component_pb2.Component(
                        slug='',
                        name='',
                        loading_dashboard_url='http://www.example.com/index?search=src',
                        abc_service_slug='abs'
                    ),
                ),
                'expected': 'Empty name'
            },
            {
                'name': 'Invalid component name',
                'request': component_pb2.CreateComponentRequest(
                    component=component_pb2.Component(
                        slug='test component',
                        name='test component',
                        loading_dashboard_url='http://www.example.com/index?search=src',
                        abc_service_slug='abs'
                    ),
                ),
                'expected': 'Spaces in component name are forbidden'
            },
            {
                'name': 'Empty abs',
                'request': component_pb2.CreateComponentRequest(
                    component=component_pb2.Component(
                        slug='name',
                        name='name',
                        loading_dashboard_url='www.example.com/index?search=src',
                        abc_service_slug=''
                    ),
                ),
                'expected': 'ABC service must be specified'
            },
            {
                'name': 'Invalid loading dashboard url',
                'request': component_pb2.CreateComponentRequest(
                    component=component_pb2.Component(
                        slug='name',
                        name='name',
                        loading_dashboard_url='www.example.com/index?search=src',
                        abc_service_slug='abs'
                    ),
                ),
                'expected': 'Invalid loading dashboard url'
            },
        ]

        for test_case in test_cases:
            self.assertEqual(
                r_component.ComponentReducer().validate_request(test_case['request']),
                test_case['expected'],
                msg=test_case['name']
            )

    def test_validation_child_requests(self):
        test_cases = [
            {
                'name': 'All good',
                'request': component_pb2.CreateComponentRequest(
                    component=component_pb2.Component(
                        loading_dashboard_url='http://www.example.com/index?search=src',
                        slug='parent_name__name',
                        name='name',
                        parent_component_name='parent_name'
                    )
                ),
                'expected': ''
            },
            {
                'name': 'Empty component name',
                'request': component_pb2.CreateComponentRequest(
                    component=component_pb2.Component(
                        loading_dashboard_url='www.example.com/index?search=src',
                        name='',
                        parent_component_name='parent_name',
                    ),
                ),
                'expected': 'Empty name'
            },
            {
                'name': 'Invalid component name',
                'request': component_pb2.CreateComponentRequest(
                    component=component_pb2.Component(
                        name='test component',
                        parent_component_name='parent_name',
                        loading_dashboard_url='http://www.example.com/index?search=src',
                    ),
                ),
                'expected': 'Spaces in component name are forbidden'
            },
            {
                'name': 'Invalid loading dashboard url',
                'request': component_pb2.CreateComponentRequest(
                    component=component_pb2.Component(
                        loading_dashboard_url='www.example.com/index?search=src',
                        name='name',
                        parent_component_name='parent_name',
                    ),
                ),
                'expected': 'Invalid loading dashboard url'
            },
        ]

        for test_case in test_cases:
            self.assertEqual(
                r_component.ComponentReducer().validate_request(test_case['request']),
                test_case['expected'],
                msg=test_case['name']
            )

    def test_component_name_validation(self):
        test_cases = [
            {
                'name': 'Empty name',
                'component_name': '',
                'expected': 'Empty name'
            },
            {
                'name': 'Good name',
                'component_name': 'name',
                'expected': ''
            },
            {
                'name': 'Name with space',
                'component_name': 'component name',
                'expected': 'Spaces in component name are forbidden'
            },
            {
                'name': 'Name with backslash',
                'component_name': 'component/name',
                'expected': 'Backslashes in component name are forbidden'
            },
            {
                'name': 'Name with _ at first position',
                'component_name': '_component_name',
                'expected': 'Underlines at first or last position are forbidden'
            },
            {
                'name': 'Name with _ at last position',
                'component_name': 'component_name_',
                'expected': 'Underlines at first or last position are forbidden'
            }
        ]

        for test_case in test_cases:
            component = component_pb2.Component(name=test_case['component_name'])
            self.assertEqual(
                r_component.ComponentReducer().validate_component_name(component),
                test_case['expected'],
                msg=test_case['name']
            )

    def test_loading_dashboard_url_validation(self):
        test_cases = [
            {
                'name': 'Empty url',
                'url': '',
                'expected': True
            },
            {
                'name': 'Valid url',
                'url': 'http://www.example.com/index?search=src',
                'expected': True
            },
            {
                'name': 'URL without scheme',
                'url': 'www.example.com/index?search=src',
                'expected': False,
            },
            {
                'name': 'URL without netloc',
                'url': 'http:///index?search=src',
                'expected': False,
            },
            {
                'name': 'URL without path',
                'url': 'http://www.example.com',
                'expected': False,
            },
        ]

        for test_case in test_cases:
            component = component_pb2.Component(loading_dashboard_url=test_case['url'])
            self.assertEqual(
                r_component.ComponentReducer().validate_loading_dashboard_url(component),
                test_case['expected'],
                msg=test_case['name']
            )
