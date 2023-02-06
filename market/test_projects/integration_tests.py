import argparse
import os
from shutil import copyfile

from lib.common import do_shell_once


def _script_path():
    return os.path.dirname(os.path.realpath(__file__))


def _replace_in_file(file_path, pattern, replacing):
    # type: (str, str, str) -> None
    if not os.path.exists(file_path):
        raise RuntimeError("File %s not found" % file_path)

    with open(file_path, "r") as f:
        lines = f.readlines()
        f.close()
        lines = map(lambda s: s.replace(pattern, replacing), lines)

    with open(file_path, "w") as f:
        f.writelines(lines)
        f.close()


def _patch_api(builder_path):
    # type: (str) -> None
    print('Patch sandbox api')
    api_path = os.path.join(builder_path, 'sandbox_api', 'scripts', 'upload.py')
    copyfile(os.path.join(_script_path(), 'fake_sandbox_api.py'), api_path)

    print('Patch requests')
    tsum_api_path = os.path.join(builder_path, 'lib', 'utils', 'tsum.py')
    _replace_in_file(tsum_api_path, "import requests", "import lib.tests.test_projects.fake_requests as requests")

    print('Patch debrelease')
    deb_lib_path = os.path.join(builder_path, 'lib', 'utils', 'deb.py')
    _replace_in_file(deb_lib_path, "lib.common.do_shell_once(['debrelease', '--to', deb_repo, '--nomail'])", "pass")


def _build_project(project_type, project_path, builder_path, user, id, **kvargs):
    # type: (str, str, str, str, str, dict) -> None
    print ('Build %s project in path %s' % (project_type, project_path))
    print ("##teamcity[blockOpened name='%s']" % project_type)
    os.chdir(project_path)

    arguments = {
        'deployTarget': 'nowhere',
        'buildModules': 'all',
        'conductorModules': 'all',
        'sandboxModules': '',
        'rpmModules': '',
        'createVersionTags': 'false',
        'versionStrategy': 'AUTO',
        'buildUser': user,
        'debRepository': 'market-common',
        'rpmDistPaths': '',
        'buildId': id,
        'customMessage': 'Test build of %s project' % project_type,
        'doNotCreateConductorTicket': "true"
    }

    arguments.update(kvargs)

    command = ['python', builder_path]

    for arg, value in arguments.iteritems():
        command.append("--%s" % arg)
        command.append(value)

    do_shell_once(command)

    print ("##teamcity[blockClosed name='%s']" % project_type)


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--builderPath', required=True, type=str, help='Project builder path')
    parser.add_argument('--buildUser', required=True, type=str, help='Teamcity build user')
    parser.add_argument('--buildId', required=True, type=str, help='Teamcity build id')

    args = parser.parse_args()

    builder_path = args.builderPath
    build_user = args.buildUser
    build_id = args.buildId

    _patch_api(builder_path)

    script_path = _script_path()
    test_projects = {
        'Common': {
            'path': os.path.join(script_path, 'common'),
            'args': {}
        },
        'Gradle single': {
            'path': os.path.join(script_path, 'gradle-single'),
            'args': {
                'sandboxModules': 'gradle-single'
            }
        },
        'Gradle multi': {
            'path': os.path.join(script_path, 'gradle-multi'),
            'args': {
                'sandboxModules': 'app-plugin-sub-project'
            }
        }
    }

    builder_file = os.path.abspath(os.path.join(builder_path, 'lib', 'main.py'))

    for name, params in test_projects.iteritems():
        _build_project(name, params.get("path"), builder_file, build_user, build_id, **params.get("args"))
