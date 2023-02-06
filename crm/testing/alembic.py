import os
import yatest


def run_alembic_command(relative_path, command):
    try:
        command_path = yatest.common.binary_path(relative_path)
        yatest.common.execute(f'{command_path} {command}', shell=True, check_exit_code=True)
    except (AttributeError, NotImplementedError):  # only for local pycharm tests
        command_path = os.path.join(os.environ.get('Y_PYTHON_SOURCE_ROOT', os.environ['PWD']), relative_path)
        import subprocess
        subprocess.call(f'{command_path} {command}', shell=True)
