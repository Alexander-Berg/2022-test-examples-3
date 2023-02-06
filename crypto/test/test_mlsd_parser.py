from crypta.lib.python.ftp.client import (
    ftp_entry,
    mlsd_parser,
)


def test_parse():
    entry = mlsd_parser.parse("modify=20200910111127;perm=adfrw;size=49143305;type=file;unique=8EU6BC4124;UNIX.group=34006;UNIX.mode=0664;UNIX.owner=34006; segments-1599736236.tar.gz")

    assert "segments-1599736236.tar.gz" == entry.name

    assert 8 ==len(entry.attrs)
    assert ftp_entry.Types.file == entry.attrs[ftp_entry.Attrs.type]
