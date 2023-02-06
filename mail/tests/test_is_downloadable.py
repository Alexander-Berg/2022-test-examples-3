import pytest

from tractor_disk.google_drive import GOOGLE_DOC_FORMATS, GoogleDrive

_MISSING_SENTINEL = object()
_SOME_EXPORT_LINKS = {"type/subtype": "url"}
_KNOWN_SPECIAL_MIME_TYPE = "application/vnd.google-apps.document"
_UNKNOWN_SPECIAL_MIME_TYPE = "application/vnd.google-apps.form"
_REGULAR_MIME_TYPE = "image/svg+xml"

assert _KNOWN_SPECIAL_MIME_TYPE in GOOGLE_DOC_FORMATS


def _without_missing(d: dict) -> dict:
    return {k: v for k, v in d.items() if v is not _MISSING_SENTINEL}


@pytest.mark.parametrize(
    "mimeType",
    [
        _MISSING_SENTINEL,
        None,
        _KNOWN_SPECIAL_MIME_TYPE,
        _UNKNOWN_SPECIAL_MIME_TYPE,
        _REGULAR_MIME_TYPE,
    ],
)
def test_file_with_some_export_links_is_downloadable(mimeType):
    file_metadata = _without_missing(
        {
            "exportLinks": _SOME_EXPORT_LINKS,
            "mimeType": mimeType,
        }
    )
    assert GoogleDrive.is_downloadable(file_metadata)


@pytest.mark.parametrize("exportLinks", [_MISSING_SENTINEL, None, {}])
def test_file_with_known_special_mime_type_is_downloadable(exportLinks):
    file_metadata = _without_missing(
        {
            "exportLinks": exportLinks,
            "mimeType": _KNOWN_SPECIAL_MIME_TYPE,
        }
    )
    assert GoogleDrive.is_downloadable(file_metadata)


@pytest.mark.parametrize("exportLinks", [_MISSING_SENTINEL, None, {}])
def test_file_with_unknown_special_mime_type_is_not_downloadable(exportLinks):
    file_metadata = _without_missing(
        {
            "exportLinks": exportLinks,
            "mimeType": _UNKNOWN_SPECIAL_MIME_TYPE,
        }
    )
    assert not GoogleDrive.is_downloadable(file_metadata)


@pytest.mark.parametrize("exportLinks", [_MISSING_SENTINEL, None, {}])
def test_file_with_regular_mime_type_is_downloadable(exportLinks):
    file_metadata = _without_missing(
        {
            "exportLinks": exportLinks,
            "mimeType": _REGULAR_MIME_TYPE,
        }
    )
    assert GoogleDrive.is_downloadable(file_metadata)
