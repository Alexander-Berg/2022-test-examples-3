# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import datetime

import mock
import pytest

from common.apps.info_center.models import ObjectLink, ObjLinkType, Info
from common.apps.info_center.push import PushSender, get_dir_pair_key
from common.data_api.sup.client import SupClient
from common.models.factories import create_info, create_station
from common.tester.utils.datetime import replace_now
from common.tester.utils.replace_setting import replace_setting


pytestmark = [pytest.mark.mongouser('module'), pytest.mark.dbuser('module')]


class TestPushSender(object):
    def test_make_receivers_query(self):
        info = create_info()
        sender = PushSender(info.id, oauth_token='42')
        sender.info = info

        create_station(id=123, __={'codes': {'esr': '5123'}})
        create_station(id=567, __={'codes': {'esr': '5567'}})

        with pytest.raises(ValueError):
            sender.make_receivers_query()

        info.linked_objects = [
            [
                ObjectLink(obj_type=ObjLinkType.STATION, obj_key=123),
                ObjectLink(obj_type=ObjLinkType.STATION, obj_key=567),
            ],
            [],
            [
                ObjectLink(obj_type=ObjLinkType.SETTLEMENT, obj_key=42),
                ObjectLink(obj_type=ObjLinkType.DIRECTION, obj_key=43),
            ]
        ]
        info.save()

        sender = PushSender(info.id, oauth_token='42')
        sender.info = info

        with replace_setting('SUBURBAN_APP_IDS', ['ru.yandex.rasp', 'ru.yandex.mobile']):
            assert (
                sender.make_receivers_query() ==
                "tag:app_id in ('ru.yandex.rasp', 'ru.yandex.mobile') && "
                "(suburban_station==5123 || suburban_station==5567) && (suburban_city==42 || suburban_direction==43)"
            )

    def test_send(self):
        info = create_info()
        create_station(id=123, __={'codes': {'esr': '5123'}})
        info.linked_objects = [[ObjectLink(obj_type=ObjLinkType.STATION, obj_key=123)]]
        info.save()

        sender = PushSender(info.id, oauth_token='42')

        now = datetime(2018, 11, 20, 13, 42)
        with mock.patch.object(SupClient, 'pushes') as m_pushes, replace_now(now), \
                replace_setting('SUBURBAN_APP_IDS', ['ru.yandex.rasp', 'ru.yandex.mobile']):

            m_pushes.side_effect = [
                '{"trace": {"resolveEvents": ["wow resolved to 42 receiver!!"]}}',
                '{}',
            ]

            sender.send('тайтл', 'текст', 'http://урл', 'картинка', 'suburban_default_device_id')

            kwargs = dict(
                project='suburban',
                image='ic_notification',
                receivers=["tag:app_id in ('ru.yandex.rasp', 'ru.yandex.mobile') && (suburban_station==5123)"],
                title='тайтл',
                text='текст',
                image_url='картинка',
                url='http://урл',
                device_id_policy='suburban_default_d8_device_id',
                install_id_policy='suburban_h6_d14_install_id',
                dry_run=False,
                high_priority=True,
            )
            assert m_pushes.call_args_list[0][1] == kwargs

            info = Info.objects.get(id=info.id)
            push = info.pushes[0]
            assert push.title == 'тайтл'
            assert push.text == 'текст'
            assert push.url == 'http://урл'
            assert push.image_url == 'картинка'
            assert push.dt_created == now
            assert push.linked_objects == info.linked_objects

    def test_send_errors(self):
        info = create_info()
        create_station(id=123, __={'codes': {'esr': '5123'}})
        info.linked_objects = [[ObjectLink(obj_type=ObjLinkType.STATION, obj_key=123)]]
        info.save()
        sender = PushSender(info.id, oauth_token='42')

        with mock.patch.object(SupClient, 'pushes') as m_pushes:
            m_pushes.side_effect = IOError('abcd')

            with pytest.raises(IOError):
                sender.send('тайтл', 'текст', 'http://урл', 'картинка', 'suburban_default_device_id')

            info = Info.objects.get(id=info.id)
            push = info.pushes[0]
            assert push.title == 'тайтл'
            assert push.text == 'текст'
            assert push.url == 'http://урл'
            assert push.image_url == 'картинка'
            assert push.linked_objects == info.linked_objects
            assert push.error == "IOError(u'abcd',)"

        info.delete()
        with pytest.raises(PushSender.Error):
            sender.send('тайтл', 'текст', 'http://урл', 'картинка', 'urgent')


def test_get_dir_pair_key():
    assert get_dir_pair_key(1, 2) == '1+2'
    assert get_dir_pair_key(2, 1) == '1+2'
