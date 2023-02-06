# -*- coding: utf-8 -*-
from hamcrest import assert_that, calling, raises

from mpfs.platform.v1.chaining.exceptions import ChainingApplyTemplateError
from mpfs.platform.utils import CaseInsensitiveDict
from test.unit.base import NoDBTestCase


class ChainingTemplateTestCase(NoDBTestCase):
    """Проверяет метод chaining.chaining_processors.ChainRequestProcessor._apply_template"""

    def setup_method(self, method):
        from mpfs.platform.v1.chaining.chaining_processors import ChainRequestProcessor
        self.processor = ChainRequestProcessor(None)
        self.content = {
            'uid': '1234',
            'files': [{'file1': 'fake'}, {'file1': {'file2': '/disk/folder/true_file'}}],
            'fake_field': 'test',
            '"333"': 'number_field'
        }
        self.headers = CaseInsensitiveDict({
            'lowercase': '/abc',
            'UpperCase': '/ABC',
            'key.with.dots': '/dots',
        })

    def test_template_correct_body(self):
        template_str = 'http://example.com/v1/{body.uid}/call?path={body.files.1.file1.file2}'
        correct_str = 'http://example.com/v1/1234/call?path=%2Fdisk%2Ffolder%2Ftrue_file'
        assert self.processor._apply_template(template_str, self.headers, self.content) == correct_str

        template_str = 'http://example.com/v1/1234/call?number={body."333"}'
        correct_str = 'http://example.com/v1/1234/call?number=number_field'
        assert self.processor._apply_template(template_str, self.headers, self.content) == correct_str

    def test_template_correct_headers(self):
        template_str = 'http://example.com/v1/{body.uid}/call?path={headers.lowercase}'
        correct_str = 'http://example.com/v1/1234/call?path=%2Fabc'
        assert self.processor._apply_template(template_str, self.headers, self.content) == correct_str

        template_str = 'http://example.com/v1/{body.uid}/call?path={headers.LOWERCASE}'
        correct_str = 'http://example.com/v1/1234/call?path=%2Fabc'
        assert self.processor._apply_template(template_str, self.headers, self.content) == correct_str

        template_str = 'http://example.com/v1/{body.uid}/call?path={headers.uppercase}'
        correct_str = 'http://example.com/v1/1234/call?path=%2FABC'
        assert self.processor._apply_template(template_str, self.headers, self.content) == correct_str

        template_str = 'http://example.com/v1/{body.uid}/call?path={headers.key.with.dots}'
        correct_str = 'http://example.com/v1/1234/call?path=%2Fdots'
        assert self.processor._apply_template(template_str, self.headers, self.content) == correct_str

    def test_template_error(self):
        template_str = 'http://example.com/v1/1234/call?number={body.missing_field}'
        assert_that(calling(self.processor._apply_template).with_args(template_str, self.headers,
                                                                      self.content), raises(KeyError))

        template_str = 'http://example.com/v1/1234/call?number={body.0}'
        assert_that(calling(self.processor._apply_template).with_args(template_str, self.headers,
                                                                      self.content), raises(KeyError))

        template_str = 'http://example.com/v1/1234/call?number={body.files.2}'
        assert_that(calling(self.processor._apply_template).with_args(template_str, self.headers,
                                                                      self.content), raises(IndexError))

        template_str = 'http://example.com/v1/1234/call?number={body.files.-1}'
        assert_that(calling(self.processor._apply_template).with_args(
            template_str, self.headers, self.content), raises(ChainingApplyTemplateError))

        template_str = 'http://example.com/v1/1234/call?number={body.files.str_key}'
        assert_that(calling(self.processor._apply_template).with_args(template_str, self.headers,
                                                                      self.content), raises(ValueError))

        template_str = 'http://example.com/v1/1234/call?number={tail.files.str_key}'
        assert_that(calling(self.processor._apply_template).with_args(
            template_str, self.headers, self.content), raises(ChainingApplyTemplateError))
