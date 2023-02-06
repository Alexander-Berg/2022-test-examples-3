# -*- coding: utf-8 -*-
from mpfs.common.static import tags
from mpfs.platform import set_api_mode
from mpfs.platform.common import PlatformRequest, PlatformClient
from mpfs.platform.fields import StringField
from mpfs.platform.routers import BaseRouter
from mpfs.platform.serializers import BaseSerializer
from test.unit.base import NoDBTestCase


class TestSerializer(BaseSerializer):
    fields = {
        'external_field': StringField(required=True, source='external_field', help_text=u'Внешнее поле, видят все'),
        'native_client_field': StringField(required=True, source='native_client_field', help_text=u'Внешнее поле, но видят только Дисковые нативные клиенты'),
        'internal_field': StringField(required=True, source='internal_field', help_text=u'Внутреннее поле, видно только в INTAPI'),
        'excluded_field': StringField(required=True, source='excluded_field', help_text=u'Исключенное поле, не видно никому'),
        'invisible_field': StringField(required=True, source='invisible_field', help_text=u'Невидимое поле, не видно никому'),
    }

    visible_fields = [
        'external_field', 'native_client_field', 'internal_field', 'excluded_field',
    ]

    native_client_fields = [
        'native_client_field',
    ]

    internal_only_fields = [
        'internal_field',
    ]

    excluded_fields = [
        'excluded_field',
    ]


class SerializerFieldTestCase(NoDBTestCase):
    value = {
        'external_field': 'external_field',
        'native_client_field': 'native_client_field',
        'internal_field': 'internal_field',
        'excluded_field': 'excluded_field',
        'invisible_field': 'invisible_field',
    }

    def teardown_class(cls):
        set_api_mode(None)
        super(SerializerFieldTestCase, cls).teardown_class()

    def __make_not_native_request(self):
        request = PlatformRequest()
        client = PlatformClient()
        client.id = 'fake_client_id'
        request.client = client
        return request

    def __make_native_request(self):
        request = PlatformRequest()
        client = PlatformClient()
        client.id = 'disk_verstka'
        request.client = client
        return request

    def test_external_client(self):
        set_api_mode(tags.platform.EXTERNAL)
        request = self.__make_not_native_request()
        router = BaseRouter()
        router.set_request(request)

        serializer = TestSerializer(obj=self.value, router=router)
        result = serializer.data

        assert 'external_field' in result
        assert 'native_client_field' not in result
        assert 'internal_field' not in result
        assert 'excluded_field' not in result
        assert 'invisible_field' not in result

    def test_native_client(self):
        set_api_mode(tags.platform.EXTERNAL)
        request = self.__make_native_request()
        router = BaseRouter()
        router.set_request(request)

        serializer = TestSerializer(obj=self.value, router=router)
        result = serializer.data

        assert 'external_field' in result
        assert 'native_client_field' in result
        assert 'internal_field' not in result
        assert 'excluded_field' not in result
        assert 'invisible_field' not in result

    def test_internal_client(self):
        set_api_mode(tags.platform.INTERNAL)
        request = self.__make_not_native_request()
        router = BaseRouter()
        router.set_request(request)

        serializer = TestSerializer(obj=self.value, router=router)
        result = serializer.data

        assert 'external_field' in result
        assert 'native_client_field' in result
        assert 'internal_field' in result
        assert 'excluded_field' not in result
        assert 'invisible_field' not in result
