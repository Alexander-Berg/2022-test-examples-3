# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import mock
import pytest

from common.test_utils.workflow import SCHEME, SomeDocument, use_registry
from common.workflow import registry, scheme
from common.workflow.process import Process


@use_registry
def test_registry():
    with pytest.raises(KeyError):
        registry.get_process('myprocess')

    with mock.patch.object(scheme, 'load_scheme', wraps=scheme.load_scheme) as m_load_scheme:
        registry.add_process('myprocess', SCHEME, SomeDocument, namespace='process.1.somepath')
        m_load_scheme.assert_called_once_with(SCHEME)

    doc = SomeDocument.objects.create()
    with mock.patch.object(registry, 'Process', wraps=Process) as m_process:
        process = registry.get_process('myprocess', doc)

    assert isinstance(process, Process)
    m_process.assert_called_once_with(scheme=SCHEME, document=doc, namespace='process.1.somepath')

    # already registered
    with pytest.raises(ValueError):
        registry.add_process('myprocess', SCHEME, SomeDocument, namespace='process.1.somepath')

    registry.clear()
    with pytest.raises(KeyError):
        registry.get_process('myprocess')


@use_registry
def test_run_process():
    doc = SomeDocument.objects.create()

    with pytest.raises(KeyError):
        registry.run_process('myprocess', doc.id)

    registry.add_process('myprocess', SCHEME, SomeDocument, namespace='process.1.somepath')
    with mock.patch.object(registry, 'get_process', wraps=registry.get_process) as m_get_process, \
            mock.patch.object(Process, 'run') as m_run, \
            mock.patch.object(Process, 'state'):

        registry.run_process('myprocess', doc.id)
        m_get_process.assert_called_once_with('myprocess', doc)
        m_run.assert_called_once_with({'process_name': 'myprocess'})

    doc.delete()
    with pytest.raises(SomeDocument.DoesNotExist):
        registry.run_process('myprocess', doc.id)
