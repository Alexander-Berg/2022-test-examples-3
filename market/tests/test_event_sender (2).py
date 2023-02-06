import socket
import time

from market.pylibrary.tsum_events.tsum_events import MicroEvent, EventStatus, MicroEvents, Event, Events


def test_microevents():
    event = MicroEvent(timeSeconds=int(time.time()), text="some text", type='salt', project='market',
                       status=EventStatus.Value('WARN'), source=socket.getfqdn(socket.gethostname()),
                       tags=['test', 'test2'])
    events = MicroEvents()
    events.microEvents.extend([event])


def test_events():
    event = Event(startTimeSeconds=int(time.time()), endTimeSeconds=int(time.time()))
    events = Events()
    events.events.extend([event])
