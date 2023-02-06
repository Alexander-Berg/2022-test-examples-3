# -*- coding: utf-8 -*-

import time
import json
import re
from datetime import datetime, timedelta

"""
украдено отсюда:
https://github.yandex-team.ru/direct/yandex-ppc-direct.traceutil/blob/master/lib/direct/traceutil/trace_format.py
"""

__all__ = [
    'Trace',
    'TraceTimes',
    'TraceProfile',
    'TraceService',
    'TraceMark',
    'TraceAnnotation',
]


def simple_struct(name, fields):
    fields = tuple(str(field) for field in fields)
    if not fields:
        raise ValueError('Simple struct must have at least one field')
    fieldlist = ', '.join(fields)
    fieldinit = '\n'.join('        self.%s = %s' % (field, field) for field in fields)
    fieldreprfmt = ', '.join('%s=%r' for field in fields)
    fieldreprlist = ', '.join('%r, self.%s' % (field, field) for field in fields)
    fieldselflist = ', '.join('self.%s' % (field,) for field in fields)
    fieldeq = '\n'.join('        if not (self.%s == other.%s):\n            return False' % (field, field) for field in fields)
    fieldne = '\n'.join('        if self.%s != other.%s:\n            return True' % (field, field) for field in fields)

    classdecl = '''\
class %(name)s(object):
    __slots__ = %(fields)r

    def __new__(cls, %(fieldlist)s):
        self = object.__new__(cls)
%(fieldinit)s
        return self

    def __repr__(self):
        return '%%s(%(fieldreprfmt)s)' %% (type(self).__name__, %(fieldreprlist)s)

    def __eq__(self, other):
        if type(self) is not type(other):
            return False
%(fieldeq)s
        return True

    def __ne__(self, other):
        if type(self) is not type(other):
            return True
%(fieldne)s
        return False

    def __getnewargs__(self):
        return (%(fieldselflist)s,)

    def __getstate__(self):
        return False

    def __setstate__(self, state):
        pass
''' % locals()
    from itertools import izip
    namespace = dict(__name__='simple_struct_%s' % (name,),
                     object=object,
                     type=type,
                     izip=izip)
    try:
        exec classdecl in namespace
    except SyntaxError, e:
        raise SyntaxError(e.message + ':\n' + classdecl)
    result = namespace[name]
    try:
        import sys
        result.__module__ = sys._getframe(1).f_globals.get('__name__', '__main__')
    except (AttributeError, ValueError):
        pass
    return result

Trace = simple_struct('Trace', (
    'trace_id',
    'parent_id',
    'span_id',
    'span_start',
    'chunk_index',
    'chunk_last',
    'chunk_start',
    'chunk_end',
    'host',
    'pid',
    'service',
    'method',
    'tags',
    'samplerate',
    'times',
    'profiles',
    'services',
    'marks',
    'annotations',
))

TraceTimes = simple_struct('TraceTimes', (
    'ela',
    'cu',
    'cs',
    'mem',
))

TraceProfile = simple_struct('TraceProfile', (
    'func',
    'tags',
    'all_ela',
    'child_ela',
    'calls',
    'obj_num',
))

TraceService = simple_struct('TraceService', (
    'service',
    'method',
    'span_id',
    'start',
    'ela',
))

TraceMark = simple_struct('TraceMark', (
    'mark',
    'message',
))

TraceAnnotation = simple_struct('TraceAnnotation', (
    'key',
    'value',
))


class Trace(Trace):
    @classmethod
    def decode(cls, msg):
        if isinstance(msg, basestring):
            msg = json.loads(msg)
        if msg[0] not in (0, 1, 2, 3):
            raise ValueError("Unexpected message format %r" % (msg[0],))
        if msg[0] < 2:
            msg = msg[:11] + [True if msg[10] == 0 else False] + msg[11:]
        if msg[0] < 3:
            msg = msg[:13] + [1] + msg[13:]
        (fmt, logtime, host, pid,
         service, method, tags,
         traceid, parentid, spanid,
         chunk_index, chunk_last, chunk_time,
         samplerate, data) = msg
        logtime = datetime.strptime(logtime, '%Y-%m-%d %H:%M:%S.%f')
        if fmt == 0:
            # convert local time to utc
            ms = logtime.microsecond
            logtime = time.mktime(logtime.timetuple())
            logtime = datetime.utcfromtimestamp(logtime)
            logtime = datetime(logtime.year, logtime.month, logtime.day, logtime.hour, logtime.minute, logtime.second, ms)
            # convert milliseconds to seconds
            chunk_time /= 1000.0
            data['times']['ela'] /= 1000.0
            data['times']['cu'] /= 1000.0
            data['times']['cs'] /= 1000.0
            for data_profile in data.get('profile', ()):
                data_profile[3] /= 1000.0
                data_profile[4] /= 1000.0
            for data_services in data.get('services', ()):
                data_services[3] /= 1000.0
                data_services[4] /= 1000.0
            for data_marks in data.get('marks', ()):
                data_marks[0] /= 1000.0
        if fmt < 2 and data.get('profile'):
            # Strip older package fields
            data['profile'] = [p[1:] for p in data['profile']]
        return cls(trace_id=traceid,
                   parent_id=parentid,
                   span_id=spanid,
                   span_start=logtime - timedelta(seconds=chunk_time),
                   chunk_index=chunk_index,
                   chunk_last=chunk_last,
                   chunk_start=logtime - timedelta(seconds=data['times']['ela']),
                   chunk_end=logtime,
                   host=host,
                   pid=pid,
                   service=service,
                   method=method,
                   tags=tags,
                   samplerate=samplerate,
                   times=TraceTimes(data['times']['ela'], data['times']['cu'], data['times']['cs'], data['times']['mem']),
                   profiles=[TraceProfile(*p) for p in data.get('profile', ())],
                   services=[TraceService(*s) for s in data.get('services', ())],
                   marks=[TraceMark(*m) for m in data.get('marks', ())],
                   annotations=[TraceAnnotation(*a) for a in data.get('annotations', ())])

    def encode(self):
        def strip_empty(d):
            for key, value in d.items():
                if not value:
                    del d[key]
            return d
        return [
            3,
            self.chunk_end.strftime('%Y-%m-%d %H:%M:%S.%f'),
            self.host,
            self.pid,
            self.service,
            self.method,
            self.tags,
            self.trace_id,
            self.parent_id,
            self.span_id,
            self.chunk_index,
            self.chunk_last,
            (self.chunk_end - self.span_start).total_seconds(),
            self.samplerate,
            strip_empty({
                'times': {'ela': self.times.ela, 'cu': self.times.cu, 'cs': self.times.cs, 'mem': self.times.mem},
                'profile': [[p.func, p.tags, p.all_ela, p.child_ela, p.calls, p.obj_num] for p in self.profiles],
                'services': [[s.service, s.method, s.span_id, s.start, s.ela] for s in self.services],
                'marks': [[m.mark, m.message] for m in self.marks],
                'annotations': [[a.key, a.value] for a in self.annotations],
            }),
        ]

    def profile_match_sum(self, pattern, field):
        """ filter profile[] elements by pattern and summarize field (for direct-log -f) """
        ret = 0.0
        rx = re.compile(pattern)
        for p in self.profiles:
            func_str = p.func + ("" if not p.tags else "/" + p.tags)
            if rx.search(func_str):
                ret += getattr(p, field)
        return ret

    @classmethod
    def merge(cls, chunks):
        """Merges multiple span chunks into a single span"""
        assert chunks, 'cannot make complete span from zero chunks'
        if len(chunks) == 1:
            # Merging single chunk is super easy!
            return chunks[0]
        if not chunks[-1].chunk_last:
            # make sure last chunk is really last
            chunks = list(chunks)
            for index, chunk in enumerate(chunks[:-1]):
                if chunk.chunk_last:
                    del chunks[index]
                    chunks.append(chunk)
                    break
        # use last chunk as a template
        trace = chunks[-1]
        # make an empty copy
        trace = cls(
            trace_id=trace.trace_id,
            parent_id=trace.parent_id,
            span_id=trace.span_id,
            span_start=min(chunk.span_start for chunk in chunks),
            chunk_index=1,
            chunk_last=True,
            chunk_start=min(chunk.chunk_start for chunk in chunks),
            chunk_end=max(chunk.chunk_end for chunk in chunks),
            host=trace.host,
            pid=trace.pid,
            service=trace.service,
            method=trace.method,
            tags=trace.tags,
            samplerate=trace.samplerate,
            times=TraceTimes(ela=None, cu=None, cs=None, mem=None),
            profiles=[],
            services=[],
            marks=[],
            annotations=[])
        # compute aggregates
        trace.times.ela = (trace.chunk_end - trace.chunk_start).total_seconds()
        trace.times.cu = sum(chunk.times.cu for chunk in chunks)
        trace.times.cs = sum(chunk.times.cs for chunk in chunks)
        trace.times.mem = sum(chunk.times.mem for chunk in chunks)
        funcindex = {}  # (func,tags) -> index
        for chunk in chunks:
            # aggregate values and preserve ordering
            for p in chunk.profiles:
                index = funcindex.get((p.func, p.tags))
                if index is None:
                    index = len(trace.profiles)
                    funcindex[(p.func, p.tags)] = index
                    target = TraceProfile(func=p.func,
                                          tags=p.tags,
                                          all_ela=p.all_ela,
                                          child_ela=p.child_ela,
                                          calls=p.calls,
                                          obj_num=p.obj_num)
                    trace.profiles.append(target)
                else:
                    target = trace.profiles[index]
                    target.all_ela += p.all_ela
                    target.child_ela += p.child_ela
                    target.calls += p.calls
                    target.obj_num += p.obj_num
            trace.services.extend(chunk.services)
            trace.marks.extend(chunk.marks)
            trace.annotations.extend(chunk.annotations)
        return trace
