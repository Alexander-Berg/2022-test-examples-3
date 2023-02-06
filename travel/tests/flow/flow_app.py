# -*- coding: utf-8 -*-

from copy import copy
import os
import tarfile

from yatest.common import network
import yatest.common as common

from travel.cpa.lib.lb_writer import LBWriter

from data import LabelAvia, LabelHotels, LabelTrain, LabelSuburban, LabelBuses, LabelTours, Snapshot


STOP_WORD = 'PLEASE_STOP'

BREAKER_LABEL_AVIA = LabelAvia.default().replace(marker=STOP_WORD)

BREAKER_LABEL_HOTELS = LabelHotels.default().replace(Label=STOP_WORD)

BREAKER_LABEL_TRAIN = LabelTrain.default().replace(LabelHash=STOP_WORD)

BREAKER_LABEL_SUBURBAN = LabelSuburban.default().replace(LabelHash=STOP_WORD)

BREAKER_LABEL_BUSES = LabelBuses.default().replace(LabelHash=STOP_WORD)

BREAKER_LABEL_TOURS = LabelTours.default().replace(LabelHash=STOP_WORD)

BREAKER_SNAPSHOT = Snapshot.default().replace(hash=STOP_WORD)


class FlowApp(object):
    topic_label_avia = 'topic_label_avia'
    topic_label_hotels = 'topic_label_hotels'
    topic_label_train = 'topic_label_train'
    topic_label_suburban = 'topic_label_suburban'
    topic_label_buses = 'topic_label_buses'
    topic_label_tours = 'topic_label_tours'
    topic_snapshot = 'topic_snapshot'

    def __init__(self, yt_helper):
        self.yt_helper = yt_helper
        self.lb_grpc_port = int(os.getenv('LOGBROKER_PORT'))

    def run_app(self, processed_snapshots, saved_snapshots, purgatory_items, labels_to_send, snapshots_to_send):
        self.yt_helper.create_tables()
        self.yt_helper.write_snapshots(processed_snapshots, 'processed_snapshots')
        self.yt_helper.write_snapshots(saved_snapshots, 'snapshots')
        self.yt_helper.write_purgatory_items(purgatory_items, 'order_purgatory')

        self._send_to_lb(labels_to_send, snapshots_to_send)

        bin_root = common.binary_path('travel/cpa/data_processing/flow')
        self._extract_libraries(bin_root)

        with network.PortManager() as pm:
            http_port = pm.get_port()
            self._prepare_properties(bin_root, http_port)
            os.chdir(bin_root)
            common.execute(self._get_java_args(bin_root), wait=True)

    def _send_to_lb(self, labels_to_send, snapshots_to_send):
        snapshots_to_send = copy(snapshots_to_send)
        snapshots_to_send.append(BREAKER_SNAPSHOT)

        writer = LBWriter('localhost', self.lb_grpc_port, 'test_id', None)

        labels_avia = labels_to_send['avia']
        labels_avia.append([BREAKER_LABEL_AVIA])
        writer.write(self.topic_label_avia, self._convert_messages(labels_avia))

        labels_hotels = labels_to_send['hotels']
        labels_hotels.append([BREAKER_LABEL_HOTELS])
        writer.write(self.topic_label_hotels, self._convert_messages(labels_hotels))

        labels_train = labels_to_send['train']
        labels_train.append([BREAKER_LABEL_TRAIN])
        writer.write(self.topic_label_train, self._convert_messages(labels_train))

        labels_suburban = labels_to_send['suburban']
        labels_suburban.append([BREAKER_LABEL_SUBURBAN])
        writer.write(self.topic_label_suburban, self._convert_messages(labels_suburban))

        labels_buses = labels_to_send['buses']
        labels_buses.append([BREAKER_LABEL_BUSES])
        writer.write(self.topic_label_buses, self._convert_messages(labels_buses))

        labels_tours = labels_to_send['tours']
        labels_tours.append([BREAKER_LABEL_TOURS])
        writer.write(self.topic_label_tours, self._convert_messages(labels_tours))

        writer.write(self.topic_snapshot, [item.as_dict() for item in snapshots_to_send])

    @staticmethod
    def _convert_messages(messages):
        converted_messages = list()
        for message in messages:
            converted_messages.append([item.as_dict() for item in message])
        return converted_messages

    @staticmethod
    def _extract_libraries(bin_root):
        # JAVA_PROGRAM classpath is delivered as directory with files when `YMAKE_JAVA_MODULES==yes` rather then
        # tar archive.
        if os.path.isdir(os.path.join(bin_root, 'travel-cpa-flow')):
            return
        tar_path = os.path.join(bin_root, 'travel-cpa-flow.tar')
        tar_target = os.path.join(bin_root, 'travel-cpa-flow')
        with tarfile.open(tar_path) as f:
            f.extractall(tar_target)

    def _prepare_properties(self, bin_root, http_port):
        properties_template_path = common.source_path('travel/cpa/tests/flow/application_properties_template.yml')
        with open(properties_template_path) as f:
            properties_template = f.read()
        properties = properties_template.format(
            http_port=http_port,
            lb_port=self.lb_grpc_port,
            yt_proxy=self.yt_helper.yt_proxy,
            yt_root=self.yt_helper.yt_root,
        )

        cfg_path = os.path.join(bin_root, 'application-tests.yml')
        with open(cfg_path, 'w') as f:
            f.write(properties)

    @staticmethod
    def _get_java_args(bin_root):
        args_template_path = common.source_path('travel/cpa/tests/flow/java_args_template.txt')
        with open(args_template_path) as f:
            args_template = f.read()
        args_text = args_template.format(
            java_bin=common.java_bin(),
            log_dir=common.output_path('app-logs'),
            lib_dir=os.path.join(bin_root, 'travel-cpa-flow'),
        )
        return args_text.split('\n')
