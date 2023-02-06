# coding: utf-8

'''
Resonance tester entrypoint.
'''

from __future__ import absolute_import
from __future__ import unicode_literals
from __future__ import division

import argparse
import logging
import math
import numpy as np
import sys
import json

import plotly.graph_objs as go
import plotly.offline as py

from google.protobuf import json_format

from search.martylib.core.date_utils import CompatibleDateTime, set_timezone

from search.resonance.tester.proto.result_pb2 import TResult


LOGGER = logging.getLogger('resonance.exporter')
PALETTE = [
    '#37bff2',
    '#169833',
    '#f6ab31',
    '#c95edd',
    '#e85b4e',
    '#409fd4',
    '#7bbf00',
    '#ff2727',
    '#80f320',
    '#a6df0c',
    '#c8c102',
    '#e39e03',
    '#f67910',
    '#ff5326',
    '#fc3245',
    '#ee1869',
    '#d6078f',
    '#b601b3',
    '#9206d3',
    '#6c16ec',
    '#4830fb',
    '#2951ff',
    '#1175f7',
    '#049be5',
    '#01bfca',
    '#0adda9',
    '#1ef283',
    '#3afd5d',
    '#5dfe3b',
    '#82f31f',
]


class ColorQueue(object):
    palette: list
    offset: int

    def __init__(self, palette=None):
        self.palette = palette or PALETTE
        self.offset = 0

    def get_next(self) -> str:
        result = self.palette[self.offset % len(self.palette)]
        self.offset += 1
        return result


def parse_args():
    parser = argparse.ArgumentParser('Resonance tester')
    parser.add_argument('-i', '--input', required=True, dest='input', help='Input test result path')
    parser.add_argument('-o', '--output', required=True, dest='output', help='Output directory')
    parser.add_argument('-t', '--target', required=False, default='Requests', dest='target', help='Default metric (Requests/Failures/etc...)')
    parser.add_argument('-s', '--step', required=False, default=1.0, type=float, dest='step', help='Step size in seconds')
    return parser.parse_args()


def configure_loggers():
    formatter = logging.Formatter('[%(levelname)s %(name)s %(asctime)s] %(message)s')
    handler = logging.StreamHandler(sys.stdout)
    handler.setFormatter(formatter)
    logging.basicConfig(level=logging.INFO, handlers=(handler,))


def resolve_payload_info(payload):
    for field, value in payload.ListFields():
        return field.name, value
    return 'unknown', ''


def collect_events(event, output: list):
    output.append((event.Begin,) + resolve_payload_info(event.Payload))
    for child in event.Scope:
        collect_events(child, output)
    output.append((event.End,) + resolve_payload_info(event.Payload))


def group_events(events, parts=20):
    events = list(events)
    events.sort(key=lambda x: x[0])
    if not events:
        return []
    interval = (events[-1][0] - events[0][0]) / parts
    result = []
    current = []
    last_time = events[0][0]
    for event in events:
        if event[0] - last_time >= interval:
            if current:
                result.append((last_time, current))
            current = []
            last_time = event[0]
        current.append(event)
    if current:
        result.append((last_time, current))
    return result


def render_groups(groups: list):
    result = []
    for group in groups:
        hover = []
        type_counter = {}
        for event in group[1]:
            type_counter[event[1]] = type_counter.get(event[1], 0) + 1
            hover.append('{}: {}'.format(event[1], str(CompatibleDateTime.fromtimestamp(event[0]))))

        text = (
            ('{}' if v == 1 else '{} x{}').format(k, v)
            for k, v in type_counter.items()
        )
        result.append({
            'captureevents': True,
            'hovertext': '<br>'.join(hover),
            'showarrow': False,
            'text': '<br>'.join(text),
            'valign': 'bottom',
            'x': CompatibleDateTime.fromtimestamp(group[0]).to_datetime(),
            'y': 0,
            'yref': 'paper',
        })
    return result


def main():
    args = parse_args()

    with open(args.input) as f:
        result = json_format.ParseDict(json.load(f), TResult())

    events_list = []
    collect_events(result.RootEvent, events_list)
    annotations = render_groups(group_events(events_list))

    fig = go.Figure(layout=go.Layout(annotations=annotations))

    color_queue = ColorQueue()
    for backend, data in result.BackendUnistat.items():
        last_x = None
        xs = []
        ys = []
        for item in data.Items:
            if not last_x or item.Time - last_x > args.step:
                last_x = item.Time
                xs.append(CompatibleDateTime.fromtimestamp(item.Time).to_datetime())
                ys.append(getattr(item, args.target, 0.0))
        fig.add_scatter(x=np.array(xs), y=np.array(ys), name=backend, line={'color': color_queue.get_next()})
    py.plot(
        fig,
        output_type='file',
        filename=args.output,
        image_filename=args.output,
        image_width=4096,
        image_height=3072,
        auto_open=False,
    )


if __name__ == '__main__':
    set_timezone('Europe/Moscow')
    configure_loggers()
    main()
