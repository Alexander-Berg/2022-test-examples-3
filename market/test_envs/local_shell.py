# coding: utf-8

import os
import re
import six
from subprocess import (
    PIPE,
    Popen,
    STDOUT,
)
from threading import Thread

EXIT_CODE_PATTERN = re.compile(r'.*exited with code\s+(?P<exit_code>\d+)')


class ExecResult(object):
    def __init__(self, exit_code=0, std_out='', std_err='', wait=False):
        self.exit_code = exit_code
        self.std_out = std_out
        self.std_err = std_err
        self.running = not wait

    def kill(self):
        self.running = False


class LocalShell(object):
    def __init__(self, istream, ostream, cwd, env=None, wait=True):
        self.data = ''
        self.cwd = cwd
        self.env = env or os.environ.copy()
        self.istream = istream
        self.ostream = ostream
        self.wait = wait

    @property
    def exit_code(self):
        if self.data:
            for row in reversed(self.data.split('\n')):
                if 'exited normally' in row:
                    return 0
                if 'exited with code' in row:
                    match = EXIT_CODE_PATTERN.search(row)
                    if match:
                        return int(match.group('exit_code'))
        return -1

    def read_from_shell(self, process):
        while True:
            readed = process.stdout.read(1)
            data = six.ensure_str(readed)
            if not data:
                break
            self.ostream.write(data)
            self.ostream.flush()
            self.data += data

    def write_to_shell(self, process, message):
        process.stdin.write(six.ensure_binary(message))
        process.stdin.flush()

    def make_shell(self, command):
        return Popen(
            command,
            stdin=PIPE, stdout=PIPE, stderr=STDOUT,
            shell=True,
            cwd=self.cwd,
            env=self.env,
        )

    def get_user_command(self):
        return self.istream.read(1).decode('utf8')

    def run(self, command):
        if isinstance(command, list):
            command = ' '.join([str(i) for i in command])

        self.ostream.write("\nStarting local terminal in {} ...\n{}\n\n".format(
            self.cwd,
            command
        ))

        shell = self.make_shell(command)
        writer = Thread(target=self.read_from_shell, args=[shell])
        writer.start()

        try:
            while writer.is_alive:
                d = self.get_user_command()
                self.write_to_shell(shell, d)
        except (IOError, EOFError):
            pass

        self.ostream.write('Exit local terminal\n')
        writer.join()
        return ExecResult(
            exit_code=self.exit_code,
            std_out=self.data,
            std_err=self.data,
            wait=self.wait
        )
