# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import pytest

from travel.rasp.library.python.common23.utils.memory_file import MemoryFile


class TestMemoryFile(object):
    def test_type_error_while_creating_memory_file(self):
        with pytest.raises(TypeError):
            MemoryFile.from_binary_string(None)

        with pytest.raises(TypeError):
            MemoryFile.from_binary_string(u"as")

        with pytest.raises(TypeError):
            MemoryFile.from_binary_string(123)

    def test_value_error_while_creating_memory_file(self):
        without_delimiters_binary_string = b'test'
        with pytest.raises(ValueError):
            MemoryFile.from_binary_string(without_delimiters_binary_string)

    def test_memory_file_read(self):
        content = b'<run></run>'
        content_type = b'text/xml'
        name = 'test.xml'
        mfile = MemoryFile(name, content_type, content)

        assert content == mfile.read()

    def test_to_binayr_string_memory_file(self):
        content = b'<run></run>'
        content_type = b'text/xml'
        name = 'test.xml'

        binary_string = b"test.xml\r\ntext/xml\r\n<run></run>"

        mfile = MemoryFile(name, content_type, content)
        mfile.check_binary_string(binary_string)
        assert mfile.to_binary_string() == binary_string

    def test_from_binary_string_memory_file(self):
        binary_string = b"test.xml\r\ntext/xml\r\n<run></run>"
        content = b'<run></run>'
        content_type = b'text/xml'
        name = 'test.xml'

        mfile = MemoryFile.from_binary_string(binary_string)

        assert name == mfile.name
        assert content == mfile.read()
        assert content_type == mfile.content_type
