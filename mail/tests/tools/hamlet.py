# coding: utf-8
from __future__ import print_function

from collections import namedtuple
from itertools import cycle, takewhile

from mail.pypg.pypg.arcadia import is_arcadia

Speech = namedtuple('Speech', ('actor', 'speech'))
Act = namedtuple('Act', ('title', 'speeches'))

FB_NS = '{http://www.gribuser.ru/xml/fictionbook/2.0}'


def read_hamlet_from_fd(fd):
    from lxml import etree as ET

    all_speaches = set()

    def read_act(act):
        title = act.find(FB_NS + 'title')[0].text

        actor = None
        prev_actor = None
        body = []

        speeches = []

        for e in act:
            if e.tag == FB_NS + 'subtitle':
                actor = e[0].text.strip()
                if prev_actor is None:
                    prev_actor = actor
                elif actor != prev_actor:
                    if body:
                        body_text = [u'\n'.join(stanza) for stanza in body]
                        body_text = u'\n\n'.join(body_text)
                        if body_text not in all_speaches:
                            speeches.append(Speech(prev_actor, body_text))
                            all_speaches.add(body_text)
                        body = []
                    prev_actor = actor
            if e.tag == FB_NS + 'poem':
                for e in e.findall(FB_NS + 'stanza'):
                    body.append([line.text for line in e])
        return Act(title, speeches)

    doc = ET.parse(fd)

    acts_iter = (a.getparent() for a in doc.findall('//' + FB_NS + 'poem'))
    acts = []
    for a in acts_iter:
        if a not in acts:
            acts.append(a)

    return [read_act(a) for a in acts]


def read_hamlet():
    if is_arcadia():
        from library.python import resource
        import io
        hamlet_file = resource.find('resfs/file/mail/pg/mdb/tests/tools/hamlet.fb2')
        assert hamlet_file, 'Resource not found'
        fd = io.BytesIO(hamlet_file)
    else:
        import os.path
        filename = os.path.join(
            os.path.dirname(__file__),
            'hamlet.fb2'
        )
        fd = open(filename)
    return read_hamlet_from_fd(fd)

    fname = os.path.join(
        os.path.dirname(__file__),
        'hamlet.fb2'
    )
    with open(fname) as fd:
        return read_hamlet_from_fd(fd)


class Hamlet(object):
    def __init__(self):
        self._acts_cycle = cycle(read_hamlet())
        self.next_act()

    def next_act(self):
        self._act = next(self._acts_cycle)
        self._act_speeches = iter(self._act.speeches)
        self._current_speech = None

    def previous_speeches(self):
        if self._current_speech is None:
            return []
        return list(
            takewhile(lambda s: s != self._current_speech, self._act.speeches)
        )

    def next_speech(self):
        try:
            self._current_speech = next(self._act_speeches)
        except StopIteration:
            # read all act - go to next
            self.next_act()
            # set to first speech,
            # cause we want return it
            self.next_speech()
        return self._current_speech


if __name__ == '__main__':
    def print_hamlet():
        for act in read_hamlet():
            print(act.title)
            for sp in act.speeches:
                print("{0}:".format(sp.actor))
                for l in sp.speech.split('\n'):
                    print("    " + l)
            print("=" * 30)

    print_hamlet()
