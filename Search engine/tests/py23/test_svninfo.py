from library.python import resource

from search.proto.svn import svn_pb2

from search.martylib.svn_utils.svninfo import parse_version
from search.martylib.test_utils import TestCase


class TestSvnInfo(TestCase):
    DATA_ROOT = 'resfs/file/search/martylib/tests/data'

    @classmethod
    def get_resource(cls, path):
        return resource.find('{}/{}'.format(cls.DATA_ROOT, path)).decode('utf-8')

    def test_strict(self):
        for incorrect_input in (
            '',
            'Git info:\nLol try Arc',
        ):
            with self.assertRaises(ValueError):
                parse_version(incorrect_input)

    # noinspection SpellCheckingInspection
    def test_svn(self):
        version = self.get_resource('svninfo-svn.txt')

        self.assertEqual(
            parse_version(version),
            svn_pb2.SvnInfo(
                origin=svn_pb2.SvnInfo.Origin.SVN,
                url='svn://arcadia.yandex.ru/arc/tags/app_host/stable-145-6/arcadia',
                last_changed_revision='6848291',
                last_changed_author='robot-srch-releaser',
                last_changed_date='2020-05-20T11:47:33.411431Z',
                build_by='sandbox',
            )
        )

    # noinspection SpellCheckingInspection
    def test_arc(self):
        version = self.get_resource('svninfo-arc.txt')

        self.assertEqual(
            parse_version(version),
            svn_pb2.SvnInfo(
                origin=svn_pb2.SvnInfo.Origin.ARC,
                arc_branch='users/roboslone/nora',
                arc_commit='4d01351b2d25a7b3bc466a1013ae0d4bda1cffb1',
                last_changed_author='zomb-sandbox-rw',
                summary='Update resource by sandbox task #683591505',
                build_by='roboslone',
            )
        )
