# -*- coding: utf-8 -*-

from __future__ import unicode_literals

import mock
from hamcrest import assert_that, has_entries

from common.utils.tanker import api, Tanker


@mock.patch.object(api.Tanker, 'list', autospec=True, return_value=[])
@mock.patch.object(api.Tanker, 'upload', autospec=True)
def test_tanker_upload(m_api_tanker_upload, m_api_tanker_list):
    tanker = Tanker(project='rasp', branch='some-branch')
    tanker.upload(keyset='some_keyset', keys={})

    m_api_tanker_list.assert_called_once_with(tanker.tanker)
    assert m_api_tanker_upload.call_count == 1
    _args, kwargs = m_api_tanker_upload.call_args
    assert_that(kwargs, has_entries(branch='some-branch'))


@mock.patch.object(api.Tanker, 'download', autospec=True, return_value='{"keysets": {}}')
def test_tanker_download(m_api_tanker_download):
    tanker = Tanker(project='rasp', branch='some-branch')
    tanker.download(keyset='some_keyset', languages=['ru'])

    assert m_api_tanker_download.call_count == 1
    _args, kwargs = m_api_tanker_download.call_args
    assert_that(kwargs, has_entries(ref='some-branch'))


def test_tanker_default_branch():
    tanker = Tanker(project='rasp', branch=None)

    assert tanker.branch == 'master'
