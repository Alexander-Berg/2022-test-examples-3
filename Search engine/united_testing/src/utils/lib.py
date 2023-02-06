import errno
import os
import sys
import time
from subprocess import check_output, PIPE, Popen
from subprocess import CalledProcessError
from color import colored


def call(cmd, ignore_rc=False, output=None):
    print colored('call {}'.format(' '.join(cmd)), attrs=['bold'])
    start = time.time()
    popen = Popen(cmd, stdout=PIPE)
    out, err = popen.communicate()
    while popen.poll() is None:
        print popen.poll()
        time.sleep(0.1)
    end = time.time()
    print 'return code: {}'.format(popen.poll())
    if output:
        output.write(out)
    else:
        print('stdout:\n{}'.format(out))
    print colored('processing time ' + str(end - start), 'yellow')
    if popen.poll():
        if ignore_rc:
            print colored('Minor error, continue', 'green')
        else:
            print colored('Error, abort!', 'red')
            sys.exit(1)
    else:
        print colored('Success', 'green')
    return popen.poll()


def verified_call(cmd, do_exit=True, shell=True):
    print colored('call ' + str(cmd), attrs=['bold'])

    start = time.time()

    try:
        output = check_output(cmd, shell=shell)
        end = time.time()
        print colored('processing time ' + str(end - start), 'yellow')
        print output
        print colored('Done!', 'green')
        return True
    except CalledProcessError as e:
        print e.output
        print colored('Error!', 'red')
        if do_exit:
            sys.exit(1)
        return False


def mkdir_p(path):
    if type(path) == list:
        for it in path:
            mkdir_p(it)
    else:
        try:
            os.makedirs(path)
        except OSError as exc:
            if exc.errno == errno.EEXIST and os.path.isdir(path):
                pass
            else:
                raise


def get_requests(args, mode):
    return os.path.join(args['{}_dir'.format(mode)], 'requests')


def get_apphost_config(args, mode):
    return os.path.join(args['{}_dir'.format(mode)], 'app_host.json')


def get_apphost_log(args, mode):
    return os.path.join(args['{}_dir'.format(mode)], 'current-eventlog-app_host')


def get_graphs_dir(args, mode):
    return os.path.join(os.path.abspath(args['{}_dir'.format(mode)]), 'graphs')
