import os
import subprocess
import contextlib


@contextlib.contextmanager
def upload_results(tracedump_path: str, tempdir: str, upload=False):
    """
    Debug context manager, which uploads binary logs to sawmill
    """
    try:
        yield
    finally:
        if upload:
            for directory, _, files in os.walk(tempdir):
                for file in files:
                    if file.endswith('.blog') and file != 'tracedump.blog':
                        subprocess.Popen(
                            (tracedump_path, '-u', os.path.join(directory, file), ),
                            stderr=subprocess.PIPE, stdout=subprocess.PIPE,
                            cwd=tempdir,
                        ).wait(timeout=5)
