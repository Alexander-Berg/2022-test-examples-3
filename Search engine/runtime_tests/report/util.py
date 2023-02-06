# -*- coding: utf-8 -*-

import re
import os
import sys
import json
import time
import zlib
import pprint
import struct
import hashlib
import urlparse

from report.proto import meta_pb2

import fabric.api
from fabric.state import env, output, win32
from fabric.network import ssh, ssh_config
from fabric.context_managers import char_buffered, quiet as quiet_manager
from fabric.io import output_loop, input_loop
from fabric.thread_handling import ThreadHandler
from fabric.utils import _pty_size, RingBuffer
import subprocess
import fabric.operations
import fabric.utils


def uniq(seq):
    seen = set()
    seen_add = seen.add
    return [x for x in seq if not (x in seen or seen_add(x))]


def GenerateBackendPort(name):
    return 22000 + struct.unpack('<L', hashlib.md5(name).digest()[:4])[0] % 1000


def validate_json(content):
    assert content, 'No json content'
    try:
        return json.loads(content)
    except ValueError as e:
        raise ValueError('Invalid json content: %s' % e)


def ParseUpperParams(name, ctx):
    assert isinstance(ctx, dict)

    for p in ['wizextra', 'rearr', 'rearr-factors', 'relev', 'snip']:
        if p in ctx:
            assert isinstance(ctx[p], list)
            if len(ctx[p]) == 1 and ctx[p][0] is None:  # no answer from wizard
                continue
            ctx[p] = ';'.join(ctx[p]).split(';')

    if name == 'YABS':
        assert ctx.get('metahost')
        bshost = ctx['metahost'][0].split(':', 1)[1]
        assert bshost
        ctx['metahost'] = urlparse.urlparse('//' + bshost)

    return ctx


def JSD(data):
        return json.dumps(data, indent=4, sort_keys=True, separators=(',', ': ')) + '\n'


def D(data):
    pprint.PrettyPrinter().pprint(data)


def D1(data):
    pprint.PrettyPrinter(depth=1).pprint(data)


def D2(data):
    pprint.PrettyPrinter(depth=2).pprint(data)


def D3(data):
    pprint.PrettyPrinter(depth=3).pprint(data)


def D4(data):
    pprint.PrettyPrinter(depth=4).pprint(data)


def PB_AddMarker(proto, grouping, marker):
    proto_data = zlib.decompress(proto)
    d = meta_pb2.TReport()
    d.ParseFromString(proto_data)

    add = 0
    for s in d.Grouping:
        if s.Attr != grouping:
            continue

        for g in s.Group:
            for doc in g.Document:
                add = 1
                new = doc.ArchiveInfo.GtaRelatedAttribute.add()
                new.Key = '_Markers'
                new.Value = marker

    assert add, 'can not found document in the groupind with name "%s"' % grouping

    return zlib.compress(d.SerializeToString())


def get_app_host_context(context=None, source='', ttype=''):
    rv = []
    if context is None:
        return rv

    for item in context:
        if not source or item.get("name") == source:
            for mref in item.get("results"):
                if not ttype or mref.get("type") == ttype:
                    rv.append(mref)

    return rv


def _fix_execute(
    channel, command, pty=True, combine_stderr=None,
    invoke_shell=False, stdout=None, stderr=None, timeout=None,
    capture_buffer_size=None,
):
    """
    Execute ``command`` over ``channel``.

    ``pty`` controls whether a pseudo-terminal is created.

    ``combine_stderr`` controls whether we call ``channel.set_combine_stderr``.
    By default, the global setting for this behavior (:ref:`env.combine_stderr
    <combine-stderr>`) is consulted, but you may specify ``True`` or ``False``
    here to override it.

    ``invoke_shell`` controls whether we use ``exec_command`` or
    ``invoke_shell`` (plus a handful of other things, such as always forcing a
    pty.)

    ``capture_buffer_size`` controls the length of the ring-buffers used to
    capture stdout/stderr. (This is ignored if ``invoke_shell=True``, since
    that completely disables capturing overall.)

    Returns a three-tuple of (``stdout``, ``stderr``, ``status``), where
    ``stdout``/``stderr`` are captured output strings and ``status`` is the
    program's return code, if applicable.
    """
    # stdout/stderr redirection
    stdout = stdout or sys.stdout
    stderr = stderr or sys.stderr

    # Timeout setting control
    timeout = env.command_timeout if (timeout is None) else timeout

    # What to do with CTRl-C?
    remote_interrupt = env.remote_interrupt

    with char_buffered(sys.stdin):
        # Combine stdout and stderr to get around oddball mixing issues
        if combine_stderr is None:
            combine_stderr = env.combine_stderr
        channel.set_combine_stderr(combine_stderr)

        # Assume pty use, and allow overriding of this either via kwarg or env
        # var.  (invoke_shell always wants a pty no matter what.)
        using_pty = True
        if not invoke_shell and (not pty or not env.always_use_pty):
            using_pty = False
        # Request pty with size params (default to 80x24, obtain real
        # parameters if on POSIX platform)
        if using_pty:
            rows, cols = _pty_size()
            channel.get_pty(width=cols, height=rows)

        # Use SSH agent forwarding from 'ssh' if enabled by user
        config_agent = ssh_config().get('forwardagent', 'no').lower() == 'yes'
        forward = None
        if env.forward_agent or config_agent:
            forward = ssh.agent.AgentRequestHandler(channel)

        # Kick off remote command
        if invoke_shell:
            channel.invoke_shell()
            if command:
                channel.sendall(command + "\n")
        else:
            channel.exec_command(command=command)

        # Init stdout, stderr capturing. Must use lists instead of strings as
        # strings are immutable and we're using these as pass-by-reference
        stdout_buf = RingBuffer(value=[], maxlen=capture_buffer_size)
        stderr_buf = RingBuffer(value=[], maxlen=capture_buffer_size)
        if invoke_shell:
            stdout_buf = stderr_buf = None

        workers = (
            ThreadHandler(
                'out', output_loop, channel, "recv",
                capture=stdout_buf, stream=stdout, timeout=timeout),
            ThreadHandler(
                'err', output_loop, channel, "recv_stderr",
                capture=stderr_buf, stream=stderr, timeout=timeout),
            ThreadHandler('in', input_loop, channel, using_pty)
        )

        if remote_interrupt is None:
            remote_interrupt = invoke_shell
        if remote_interrupt and not using_pty:
            remote_interrupt = False

        while True:
            if channel.exit_status_ready():
                break
            else:
                # Check for thread exceptions here so we can raise ASAP
                # (without chance of getting blocked by, or hidden by an
                # exception within, recv_exit_status())
                for worker in workers:
                    worker.raise_if_needed()
            try:
                time.sleep(ssh.io_sleep)
            except KeyboardInterrupt:
                if not remote_interrupt:
                    raise
                channel.send('\x03')

        # Obtain exit code of remote program now that we're done.
        status = channel.recv_exit_status()

        # Wait for threads to exit so we aren't left with stale threads
        for worker in workers:
            worker.thread.join()
            worker.raise_if_needed()

        # Close channel
        channel.close()
        # Close any agent forward proxies
        if forward is not None:
            forward.close()

        # Update stdout/stderr with captured values if applicable
        if not invoke_shell:
            # stdout_buf = ''.join(stdout_buf)
            stdout_buf = ''.join(stdout_buf)
            stderr_buf = ''.join(stderr_buf).strip()

        # Tie off "loose" output by printing a newline. Helps to ensure any
        # following print()s aren't on the same line as a trailing line prefix
        # or similar. However, don't add an extra newline if we've already
        # ended up with one, as that adds a entire blank line instead.
        if (
            output.running and
            (output.stdout and stdout_buf and not stdout_buf.endswith("\n")) or
            (output.stderr and stderr_buf and not stderr_buf.endswith("\n"))
        ):
            print ""

        return stdout_buf, stderr_buf, status


fabric.operations._execute = _fix_execute


def _fix_local(command, capture=False, shell=None, quiet=False):
    """
    Run a command on the local system.

    `local` is simply a convenience wrapper around the use of the builtin
    Python ``subprocess`` module with ``shell=True`` activated. If you need to
    do anything special, consider using the ``subprocess`` module directly.

    ``shell`` is passed directly to `subprocess.Popen
    <http://docs.python.org/library/subprocess.html#subprocess.Popen>`_'s
    ``execute`` argument (which determines the local shell to use.)  As per the
    linked documentation, on Unix the default behavior is to use ``/bin/sh``,
    so this option is useful for setting that value to e.g.  ``/bin/bash``.

    `local` is not currently capable of simultaneously printing and
    capturing output, as `~fabric.operations.run`/`~fabric.operations.sudo`
    do. The ``capture`` kwarg allows you to switch between printing and
    capturing as necessary, and defaults to ``False``.

    When ``capture=False``, the local subprocess' stdout and stderr streams are
    hooked up directly to your terminal, though you may use the global
    :doc:`output controls </usage/output_controls>` ``output.stdout`` and
    ``output.stderr`` to hide one or both if desired. In this mode, the return
    value's stdout/stderr values are always empty.

    When ``capture=True``, you will not see any output from the subprocess in
    your terminal, but the return value will contain the captured
    stdout/stderr.

    In either case, as with `~fabric.operations.run` and
    `~fabric.operations.sudo`, this return value exhibits the ``return_code``,
    ``stderr``, ``failed``, ``succeeded``, ``command`` and ``real_command``
    attributes. See `run` for details.

    `~fabric.operations.local` will honor the `~fabric.context_managers.lcd`
    context manager, allowing you to control its current working directory
    independently of the remote end (which honors
    `~fabric.context_managers.cd`).

    .. versionchanged:: 1.0
        Added the ``succeeded`` and ``stderr`` attributes.
    .. versionchanged:: 1.0
        Now honors the `~fabric.context_managers.lcd` context manager.
    .. versionchanged:: 1.0
        Changed the default value of ``capture`` from ``True`` to ``False``.
    .. versionadded:: 1.9
        The return value attributes ``.command`` and ``.real_command``.
    """
    manager = fabric.operations._noop
    if quiet:
        manager = quiet_manager
    with manager():
        given_command = command
        # Apply cd(), path() etc
        with_env = fabric.operations._prefix_env_vars(command, local=True)
        wrapped_command = fabric.operations._prefix_commands(with_env, 'local')
        if output.debug:
            print("[localhost] local: %s" % (wrapped_command))
        elif output.running:
            print("[localhost] local: " + given_command)
        # Tie in to global output controls as best we can; our capture argument
        # takes precedence over the output settings.
        dev_null = None
        if capture:
            out_stream = subprocess.PIPE
            err_stream = subprocess.PIPE
        else:
            dev_null = open(os.devnull, 'w+')
            # Non-captured, hidden streams are discarded.
            out_stream = None if output.stdout else dev_null
            err_stream = None if output.stderr else dev_null
        try:
            cmd_arg = wrapped_command if win32 else [wrapped_command]
            p = subprocess.Popen(cmd_arg, shell=True, stdout=out_stream,
                                 stderr=err_stream, executable=shell,
                                 close_fds=(not win32))
            (stdout, stderr) = p.communicate()
        finally:
            if dev_null is not None:
                dev_null.close()
        # Handle error condition (deal with stdout being None, too)
        out = fabric.operations._AttributeString(stdout if stdout else "")
        err = fabric.operations._AttributeString(stderr.strip() if stderr else "")
        out.command = given_command
        out.real_command = wrapped_command
        out.failed = False
        out.return_code = p.returncode
        out.stderr = err
        if p.returncode not in env.ok_ret_codes:
            out.failed = True
            msg = "local() encountered an error (return code %s) while executing '%s'" % (p.returncode, command)
            fabric.utils.error(message=msg, stdout=out, stderr=err)
        out.succeeded = not out.failed
        # If we were capturing, this will be a string; otherwise it will be None.
        return out


fabric.api.local = _fix_local


class singleton:
    def __init__(self, aClass):  # On @ decoration
        self.aClass = aClass
        self.instance = None

    def __call__(self, *args, **kwargs):  # On instance creation
        if self.instance is None:
            self.instance = self.aClass(*args, **kwargs)  # One instance per class
        return self.instance


class AbstractLog(object):
    def __init__(self, subject, sandbox, verbose):
        self.subject = subject
        self.sandbox = sandbox
        self.verbose = verbose

        self.skip_first = 0
        self.truncated = []
        self.buf = []
        self.size = 0

    def run(self, cmd):
        kwargs = {}
        if self.verbose < 3:
            kwargs['quiet'] = True

        if self.sandbox:
            out_line = fabric.api.local(cmd, capture=True, **kwargs)
        else:
            out_line = fabric.api.run(cmd, **kwargs)

        if out_line.failed:
            msg = "Error!\n\nRequested: %s\nExecuted: %s\nReturn value: %s\nSTDOUT: %s\nSTDERR: %s" % (
                out_line.command, out_line.real_command, out_line.return_code, out_line.stdout, out_line.stderr
            )
            fabric.utils.error(message=msg)
        return out_line

    def get(self, *args):
        raise NotImplemented("TODO")


class TextLog(AbstractLog):
    def __init__(self, subject, sandbox, verbose):
        super(TextLog, self).__init__(subject, sandbox, verbose)

        cmd = """import sys
f=open("{path}")
f.seek(0,2)
size=f.tell()
skip_first = 0
if size:
    f.seek(size-1)
    char = f.read(1)
    if char != chr(10):
        skip_first = 1
sys.stdout.write("%s %s" % (size, skip_first))
""".format(path=self.subject)
        cmd = "echo '%s' | python" % cmd

        line = self.run(cmd).split(' ', 1)
        self.size = int(line[0])
        self.skip_first = int(line[1])

    def __readlines(self):
        cmd = """import sys
f=open("{path}")
f.seek({size})
l="".join(f.readlines())
sys.stdout.write("%s %s" % (len(l), l))
""".format(path=self.subject, size=self.size)
        cmd = "echo '%s' | python" % cmd

        (size, l) = self.run(cmd).split(' ', 1)
        # farbric replace \n by \r\n, so size != len(l)
        self.size += int(size)
        return l.splitlines(1)

    def readline(self, wait=3):
        step = 0.5
        while True:
            if self.buf:
                break

            chunk = self.__readlines()
            if self.skip_first:
                # если при открытии файла не вся строка успела записаться, то надо пропустить первую строку
                if chunk and chunk[0][-1] == '\n':
                    self.skip_first = 0
                    chunk.pop(0)

            if not self.skip_first:
                if chunk:
                    if chunk[0][-1] != '\n':
                        self.truncated.extend(list(chunk.pop(0)))
                    elif self.truncated:
                        chunk[0] = ''.join(self.truncated) + chunk[0]
                        self.truncated[:] = ''

                if chunk and chunk[-1][-1] != '\n':
                    self.truncated.extend(list(chunk.pop()))

                if chunk:
                    self.buf.extend(chunk)
                    break

            if wait <= 0:
                assert len(self.buf), "can not get all characters: %s" % ''.join(self.truncated)

            wait -= step
            print '...waiting apache flushing logs'
            time.sleep(step)

        return self.buf.pop(0)

    def record(self, regex, wait=3):
        assert regex, 'No search marker (no reqid)'

        # regex is re.compile(pattern)
        if isinstance(regex, basestring):
            regex = re.compile(re.escape(regex))
        else:
            raise ValueError("TODO implement regex for: " + str(regex.__class__) + "\n\n" + str(regex))

        rec = []
        while True:
            line = self.readline(wait)
            if not line:
                break

            if regex.search(line):
                rec.append(line)
                break

        assert len(rec), 'can not find record by regex %s' % regex.pattern

        return rec


class EventLog(AbstractLog):
    def record(self, ts_start, ts_end):
        assert ts_start and ts_end and ts_start > 0 and ts_end > ts_start
        reader, path = map(self.subject.get, ('eventlog_reader', 'logs.eventlog'))
        cmd = ' '.join((reader, '-s', str(ts_start), '-e', str(ts_end), path))
        out = self.run(cmd)
        assert out, cmd + "\nNo eventlog"
        return out.split('\n')

    @staticmethod
    def parse(records):
        return filter(bool, [filter(len, map(str.strip, x.split('\t'))) for x in records])

    def get(self, *args):
        records = self.record(*args)

        parsed = self.parse(records)
        for e in parsed:
            assert len(e) >= 3, 'NOT IMPLEMENTED EVENT: ' + str(e)
            assert e[2] != 'ReportError', e  # FATAL_ERROR_MARKER is in content

        eventlog = {
            "raw": records,
            "parsed": parsed,
        }
        return eventlog


class ProfileLog(TextLog):
    def get(self, reqid):
        record = self.record(reqid)

        profile = {
            "reqid": None,
            "timings": {},
            "meta": {},
            "blocks": {},
        }
        timings_raw, meta_raw, blocks_raw = record[0].split(' ')

        for l in timings_raw.split('@@'):
            kv = l.split(':', 1)
            assert len(kv) == 2, l
            k, v = kv
            assert k not in profile['timings']
            profile['timings'][k] = v.split(':')

            if k.startswith('total_'):
                assert not profile['reqid']
                profile['reqid'] = k.split('_', 1)[1]

        for l in meta_raw.split('@@'):
            kv = l.split('=', 1)
            assert kv, l
            k = kv[0]
            v = kv[1] if len(kv) == 2 else None
            profile['meta'][k] = v

        for b in blocks_raw.split('@@'):
            kv = b.split('=', 1)
            assert len(kv) == 2, b
            k, v = kv
            assert k not in profile['blocks'], b
            profile['blocks'][k] = v

        return profile


class AccessLog(TextLog):
    __parse = re.compile(
        r'(.*) - - \[(.*)\] "(GET|POST) (.*?) (HTTP/1.[01])" (\d*) (.*) '
        r'(https?|-) "([\d,]*|-)" "(.*?)" "(.*?)" "(.*?)" "(.*?)"'
    )

    def get(self, reqid):
        record = self.record(reqid)

        match = self.__parse.match(record[0])
        assert match, "can not parse record: %s by regex: %s" % (record[0], self.__parse.pattern)

        fields = match.groups(0)
        parsed = {
            "ip": fields[0],
            "datetime": fields[1],
            "method": fields[2],
            "url": fields[3],
            "version": fields[4],
            "status": fields[5],
            "protocol": fields[7],
            "test_id": fields[8],
            "headers": fields[9],
            "redirect": fields[10],
            "flags": fields[11],
        }

        return parsed


class ReqansLog(TextLog):
    # farbric replace \n by \r\n
    __start = re.compile(r'^REQANS-START\r?$')
    __end = re.compile(r'^REQANS-END\r?$')

    def record(self, regex, wait=3):
        # regex is re.compile(pattern)
        if type(regex) == str:
            regex = re.compile(re.escape(regex))

        rec = []
        start, end = 0, 0
        while True:
            line = self.readline(wait)
            if not line:
                break

            if self.__start.search(line):
                assert not start, 'corruped record: %s%s, no end marker: %s' % (rec, line, self.__end.pattern)

                start = 1
                rec.append(line)
                continue

            if start:
                rec.append(line)
                if self.__end.search(line):
                    end = 1
                    for l in rec:
                        if regex.search(l):
                            return rec
                    # собрали запись, но не нашли нужной, ещем дальше
                    rec[:] = ''
                    start = end = 0

        assert len(rec) and start and end, 'can not find record for reqans_log: %s' % rec
        return rec

    def get(self, reqid):
        record = self.record(reqid)
        assert len(record)

        reqans = {
            'req': {},
            'url': [],
        }

        for l in record:
            if l.startswith('url='):
                reqans['url'].append(l)
            if l.startswith('req='):
                reqans['req'] = dict([p.split('=', 1) for p in l.split('@@')])

        return reqans


class XmlReqansLog(ReqansLog):
    pass


class SearchAbuseLog(TextLog):
    def get(self, reqid):
        return self.record(reqid)[0].split('\t')


ProfileLog = singleton(ProfileLog)
AccessLog = singleton(AccessLog)
ReqansLog = singleton(ReqansLog)
XmlReqansLog = singleton(XmlReqansLog)
SearchAbuseLog = singleton(SearchAbuseLog)
EventLog = singleton(EventLog)
