# -*- coding: utf-8 -*-

from mpfs.common.util.filetypes import MediaType
from mpfs.core.lenta.utils import LentaMediaType


def test_convert_to_lenta_media_type():
    assert LentaMediaType.convert_to_lenta_media_type(MediaType.AUDIO) == LentaMediaType.AUDIO
    assert LentaMediaType.convert_to_lenta_media_type(MediaType.DOCUMENT) == LentaMediaType.DOCUMENT
    assert LentaMediaType.convert_to_lenta_media_type(MediaType.IMAGE) == LentaMediaType.IMAGE
    assert LentaMediaType.convert_to_lenta_media_type(MediaType.VIDEO) == LentaMediaType.VIDEO

    assert LentaMediaType.convert_to_lenta_media_type(MediaType.BACKUP) == LentaMediaType.OTHER
    assert LentaMediaType.convert_to_lenta_media_type(MediaType.COMPRESSED) == LentaMediaType.OTHER
    assert LentaMediaType.convert_to_lenta_media_type(MediaType.DATA) == LentaMediaType.OTHER
    assert LentaMediaType.convert_to_lenta_media_type(MediaType.DEVELOPMENT) == LentaMediaType.OTHER
    assert LentaMediaType.convert_to_lenta_media_type(MediaType.DISK_IMAGE) == LentaMediaType.OTHER
    assert LentaMediaType.convert_to_lenta_media_type(MediaType.ENCODED) == LentaMediaType.OTHER
    assert LentaMediaType.convert_to_lenta_media_type(MediaType.FONT) == LentaMediaType.OTHER
    assert LentaMediaType.convert_to_lenta_media_type(MediaType.SETTINGS) == LentaMediaType.OTHER
    assert LentaMediaType.convert_to_lenta_media_type(MediaType.TEXT) == LentaMediaType.OTHER
    assert LentaMediaType.convert_to_lenta_media_type(MediaType.WEB) == LentaMediaType.OTHER
    assert LentaMediaType.convert_to_lenta_media_type(MediaType.EXECUTABLE) == LentaMediaType.OTHER
    assert LentaMediaType.convert_to_lenta_media_type(MediaType.SPREADSHEET) == LentaMediaType.OTHER
    assert LentaMediaType.convert_to_lenta_media_type(MediaType.FLASH) == LentaMediaType.OTHER
    assert LentaMediaType.convert_to_lenta_media_type(MediaType.BOOK) == LentaMediaType.OTHER
    assert LentaMediaType.convert_to_lenta_media_type(MediaType.UNKNOWN) == LentaMediaType.OTHER


def test_convert_from_lenta_media_type():
    assert LentaMediaType.convert_from_lenta_media_type(LentaMediaType.AUDIO) == [MediaType.AUDIO]
    assert LentaMediaType.convert_from_lenta_media_type(LentaMediaType.VIDEO) == [MediaType.VIDEO]
    assert LentaMediaType.convert_from_lenta_media_type(LentaMediaType.DOCUMENT) == [MediaType.DOCUMENT]
    assert LentaMediaType.convert_from_lenta_media_type(LentaMediaType.IMAGE) == [MediaType.IMAGE]
    assert LentaMediaType.convert_from_lenta_media_type(LentaMediaType.OTHER) == sorted([
        MediaType.BACKUP, MediaType.COMPRESSED, MediaType.DATA,
        MediaType.DEVELOPMENT, MediaType.DISK_IMAGE, MediaType.ENCODED,
        MediaType.FONT, MediaType.SETTINGS, MediaType.TEXT,
        MediaType.WEB, MediaType.EXECUTABLE, MediaType.SPREADSHEET,
        MediaType.FLASH, MediaType.BOOK, MediaType.UNKNOWN
    ])
