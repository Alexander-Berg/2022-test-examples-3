# coding: utf8
from common.workflow.process import Process
from common.workflow.scheme import load_scheme


def _make_action_scheme(action, events_states):
    states = {event_state: {} for event_state in events_states}
    states['initial'] = {
        'do': action,
        'transitions': {event: event_state for event_state, event in events_states.items()}
    }
    return load_scheme({'states': states})


class _StateActionProcess(Process):
    def __init__(self, action, events, document):
        self._events_states = {event + '.state': event for event in events}
        super(_StateActionProcess, self).__init__(
            _make_action_scheme(action, self._events_states),
            document=document
        )

    def run_processes(self):
        return self.run_state_action()

    def run(self):
        state = super(_StateActionProcess, self).run()
        return self._events_states.get(state, 'noevent.{}'.format(state))


def process_state_action(action, action_events, document):
    process = _StateActionProcess(action, action_events, document)
    event = process.run()
    document = process.document
    return event, document


def acquire_lock(process):
    return process.document_locker._acquire_lock()
