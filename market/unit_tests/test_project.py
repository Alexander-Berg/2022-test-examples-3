from unittest import TestCase

import mock

from lib import project
from lib.project import Project, Module
from lib.tests import test_utils


class TestProject(TestCase):
    def _compare_projects(self, fst, snd):
        # type: (Project, Project) -> None
        self.assertEqual(fst.name, snd.name)
        self.assertEqual(fst.path, snd.path)
        self.assertEqual(len(fst.get_modules()), len(snd.get_modules()))

        for fst_mod in fst.get_modules():
            snd_mod = snd.get_module(fst_mod.name)
            self.assertEqual(fst_mod.name, snd_mod.name)
            self.assertEqual(fst_mod.project.name, snd_mod.project.name)
            self.assertEqual(fst_mod.get_path(), snd_mod.get_path())
            self.assertEqual(fst_mod.type, snd_mod.type)

    def test_read_common_project_path(self):
        path = test_utils.COMMON_PROJECT_PATH
        common_project = project.read_project_path(path)
        expected = Project(path)
        expected.check_and_add_module(Module('common', Module.Type.COMMON))

        self._compare_projects(expected, common_project)

    def test_read_multi_project_path(self):
        modules = ['app-plugin-sub-project', 'deb-plugin-sub-project']
        path = test_utils.GRADLE_MULTI_PROJECT_PATH

        with mock.patch('lib.project.gradle') as gradle:
            gradle.get_project_modules.return_value = modules
            common_project = project.read_project_path(path)

        expected = Project(path)
        for mod in modules:
            expected.check_and_add_module(Module(mod, Module.Type.GRADLE))

        self._compare_projects(expected, common_project)

    def test_read_single_project_path(self):
        path = test_utils.GRADLE_SINGLE_PROJECT_PATH
        with mock.patch('lib.project.gradle') as gradle:
            gradle.get_project_modules.return_value = ['gradle-single']
            common_project = project.read_project_path(path)

        expected = Project(path)
        expected.check_and_add_module(Module('gradle-single', Module.Type.GRADLE))

        self._compare_projects(expected, common_project)
