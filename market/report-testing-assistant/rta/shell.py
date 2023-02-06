import logging
import os
import subprocess


def detach(command):
    logging.debug("Detaching `{}`".format(subprocess.list2cmdline(command)))
    return subprocess.Popen(command)


def list_folders(parent):
    try:
        children = os.walk(parent).next()[1]
    except StopIteration:
        return []
    return [child for child in children if os.path.isdir(os.path.join(parent, child))]


def make_folder(path):
    run(["mkdir", "-p", path])


def remove_folder(path):
    return run(["rm", "-rf", path])


def render(command):
    return subprocess.list2cmdline(command)


def run(command):
    logging.debug("Running `{}`".format(render(command)))
    process = subprocess.Popen(command, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    output, error = process.communicate()
    if process.returncode != 0:
        raise RuntimeError(error)


def run_into_file(command, output):
    logging.debug("Running `{0}` > {1}".format(render(command), output))
    with open(output, "w") as stream:
        process = subprocess.Popen(command, stdout=stream)
        _, error = process.communicate()
        if process.returncode != 0:
            raise RuntimeError(error)


def scopy(key, source, target):
    run(["scp", "-i", key, source, target])
