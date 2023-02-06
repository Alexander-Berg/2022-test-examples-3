# -*- coding: utf-8 -*-


def test_labels_common(session_context):
    session_context.add_label(awaited=True)
    session_context.add_label(stick=True)
    session_context.add_label(awaited=True, stick=True)
    session_context.add_label()
    session_context.add_label(stick=True)
    session_context.add_label(awaited=True)
    session_context.add_label(stick=True)
    session_context.add_label(stick=True)
    session_context.add_label(stick=True)
    session_context.add_label(awaited=True)
    session_context.check_labels()
