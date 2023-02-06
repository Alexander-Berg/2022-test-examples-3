import os

from lib.build import BuildResult
from lib.project import Project, Module
from lib.release import ReleaseResult
from lib.utils.conductor import ConductorTicket
from lib.utils.deb import DebPackage
from lib.utils.rpm import RpmPackage
from lib.utils.sandbox import SandboxResource, SandboxTicket


def get_test_projects_path():
    # TODO: uncomment after moving to arcadia
    # try:
    #     path = yatest.common.test_source_path('test_projects')
    # # except NotImplementedError:
    path = os.path.join(os.path.abspath(os.path.dirname(__file__)), 'test_projects')

    return path


TEST_PROJECTS_PATH = get_test_projects_path()
COMMON_PROJECT_PATH = os.path.join(TEST_PROJECTS_PATH, 'common')
GRADLE_MULTI_PROJECT_PATH = os.path.join(TEST_PROJECTS_PATH, 'gradle-multi')
GRADLE_MULTI_APP_PROJECT_PATH = os.path.join(TEST_PROJECTS_PATH, 'gradle-multi', 'app-plugin-sub-project')
GRADLE_MULTI_DEB_PROJECT_PATH = os.path.join(TEST_PROJECTS_PATH, 'gradle-multi', 'deb-plugin-sub-project')
GRADLE_SINGLE_PROJECT_PATH = os.path.join(TEST_PROJECTS_PATH, 'gradle-single')
GRADLE_WITHOUT_WRAPPER = os.path.join(TEST_PROJECTS_PATH, 'gradle-without-wrapper')


def check_method_calls(test, mock_method, expected):
    actual = map(lambda mc: mc[0], mock_method.call_args_list)
    test.assertListEqual(actual, expected)


def build_simple_project():
    # type: () -> (Project, Module, Module)
    prj = Project('/path_to_project')

    build_mod = Module('my_module', Module.Type.GRADLE)
    build_mod._path = '/path_to_project/module_subpath'
    build_mod.needs_to_be_built = True
    build_mod.is_conductor = True
    build_mod.is_rpm = True
    build_mod.is_sandbox = True
    build_mod.sandbox_res_type = 'MY_TYPE'
    prj.check_and_add_module(build_mod)

    not_build_mod = Module('not_build_mod', Module.Type.COMMON)
    not_build_mod._path = '/non_empty_path'
    not_build_mod.needs_to_be_built = False
    not_build_mod.is_conductor = True
    not_build_mod.is_rpm = True
    not_build_mod.is_sandbox = True
    not_build_mod.sandbox_res_type = 'NOT_MY_TYPE'

    prj.check_and_add_module(not_build_mod)

    return prj, build_mod, not_build_mod


def build_simple_build_results():
    """
    :rtype: list of BuildResult
    """
    deb_pack = BuildResult('my_module', BuildResult.PackageType.DEB, DebPackage('deb-package', '1.1'))

    rpm_pack = BuildResult('my_module', BuildResult.PackageType.RMP, RpmPackage('rpm-package', '1.1'))
    rpm_pack_test = BuildResult('my_module', BuildResult.PackageType.RMP, RpmPackage('rpm-package-for-test', '1.1'))

    sb_resource = SandboxResource('MY_TYPE', 42, '2.1', SandboxTicket('https://sb-url', 222))
    sb_pack = BuildResult('my_module', BuildResult.PackageType.SANDBOX, sb_resource)

    build_results = [deb_pack, rpm_pack, rpm_pack_test, sb_pack]
    return build_results


def build_simple_release_results():
    """
    :rtype: list of ReleaseResult
    """
    c_ticket = ConductorTicket('https://c-url', 111)
    return [
        ReleaseResult('my_module', ReleaseResult.ReleaseType.CONDUCTOR_TICKET, c_ticket),
        ReleaseResult('my_module', ReleaseResult.ReleaseType.CHANGELOG, "* First change\nsecond line\n* Second change"),
    ]
