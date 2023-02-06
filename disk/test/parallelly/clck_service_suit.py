# -*- coding: utf-8 -*-

import mock
import unittest
import urllib

from nose_parameterized import parameterized

from mpfs.core.services.clck_service import Clck


class ClckServiceTestCase(unittest.TestCase):

    # образцовый урл, который умеет резолвить кликер, мы стараемся в нашем сервисе все приводить к подобному виду
    exemplary_short_url = 'https://yadi.sk/d/EZiDUapAympaL/'
    full_url_corresponding_to_exemplary_short_url = (
        'https://disk.yandex.net/disk/public/?hash=FhgCZqDAr0UZWu14bVpB6fkwchRf5V/n8yVNb34j0tA%3D'
    )

    @parameterized.expand([
        ('type_with_trailing_slash', 'https://yadi.sk/d/'),
        ('type_without_trailing_slash', 'https://yadi.sk/d'),
        ('domain_with_trailing_slash', 'https://yadi.sk/'),
        ('domain_without_trailing_slash', 'https://yadi.sk'),
    ])
    def test_short_url_to_public_hash_raises_value_error(self, case_name, url):
        u"""Протестировать, что URL неправильного вида вызовет ошибку."""
        with self.assertRaises(ValueError):
            Clck().short_url_to_public_hash(url)

    @parameterized.expand([
        ('without_relative_path_with_trailing_slash', exemplary_short_url,
         urllib.unquote('FhgCZqDAr0UZWu14bVpB6fkwchRf5V/n8yVNb34j0tA%3D')),

        ('without_relative_path_without_trailing_slash', exemplary_short_url.rstrip('/'),
         urllib.unquote('FhgCZqDAr0UZWu14bVpB6fkwchRf5V/n8yVNb34j0tA%3D')),

        ('with_relative_path_to_dir_with_trailing_slash', exemplary_short_url + 'path/to/relative/dir/',
         urllib.unquote('FhgCZqDAr0UZWu14bVpB6fkwchRf5V/n8yVNb34j0tA%3D') + ':/path/to/relative/dir/'),

        ('with_relative_path_to_dir_without_trailing_slash', exemplary_short_url + 'path/to/relative/dir',
         urllib.unquote('FhgCZqDAr0UZWu14bVpB6fkwchRf5V/n8yVNb34j0tA%3D') + ':/path/to/relative/dir'),

        ('with_relative_path_to_file_without_trailing_slash', exemplary_short_url + 'path/to/relative/file.jpg',
         urllib.unquote('FhgCZqDAr0UZWu14bVpB6fkwchRf5V/n8yVNb34j0tA%3D') + ':/path/to/relative/file.jpg')
    ])
    def test_short_url_to_public_hash_default(self, case_name, test_url, expected_public_hash):
        u"""Протестировать резолвинг короткого URL корректного вида разных типов."""
        with mock.patch.object(
            Clck, 'short_url_to_full_url',
            return_value=self.full_url_corresponding_to_exemplary_short_url
        ) as mock_short_url_to_full_url:
            public_hash = Clck().short_url_to_public_hash(test_url)
            assert mock_short_url_to_full_url.called
            args, kwargs = mock_short_url_to_full_url.call_args
            (short_url,) = args
            assert short_url == self.exemplary_short_url
            assert public_hash == expected_public_hash
