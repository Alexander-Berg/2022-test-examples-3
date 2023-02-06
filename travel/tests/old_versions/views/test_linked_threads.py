# -*- coding: utf-8 -*-

from datetime import date

from django.test.client import Client
from lxml import etree

from common.models.schedule import RThreadType
from common.utils.date import RunMask

from common.tester.factories import create_thread
from common.tester.testcase import TestCase
from common.tester.utils.datetime import replace_now


class TestLinkedThreads(TestCase):
    @replace_now('2000-01-01 00:00:00')
    def test_linked_threads(self):
        client = Client()
        base_thread_uid = '123'
        thread_uid = '456'
        start_d = date(2000, 2, 1)
        end_d = date(2000, 2, 10)

        base_thread = create_thread(uid=base_thread_uid, title='thread_title', year_days=RunMask.range(start_d, end_d),
                                    type=RThreadType.objects.get(id=RThreadType.BASIC_ID))
        response = client.get('/export/v2/suburban/thread/{}/linked/'.format(base_thread_uid))
        assert response.status_code == 200
        threads_xml = etree.fromstring(response.content)

        assert threads_xml.tag == 'threads'
        threads = threads_xml.findall('thread')
        assert len(threads) == 1
        assert threads[0].attrib['uid'] == base_thread_uid

        masks = [el for el in threads_xml.xpath('thread/schedule/mask')]
        assert len(masks) == 12
        assert masks[1].attrib['days'] == '1' * 9 + '0' * (31 - 9)
        for mask in [masks[0]] + masks[2:]:
            assert mask.attrib['days'] == '0' * 31

        thread = create_thread(uid=thread_uid, title='thread_title', basic_thread=base_thread,
                               type=RThreadType.objects.get(id=RThreadType.CHANGE_ID))
        response = client.get('/export/v2/suburban/thread/{}/linked'.format(base_thread_uid))
        threads_xml = etree.fromstring(response.content)
        threads = threads_xml.findall('thread')
        assert len(threads) == 2
        assert threads[0].attrib['uid'] == base_thread_uid
        assert threads[1].attrib['uid'] == thread_uid

        response = client.get('/export/v2/suburban/thread/{}/linked'.format(thread_uid))
        threads_xml = etree.fromstring(response.content)
        assert threads_xml.tag == 'error'
        text = u'Переданный uid соответствует нитке с типом {}. Требуется нитка с типом {}.'.format(
            thread.type.code, base_thread.type.code)
        assert threads_xml.attrib['text'] == text
