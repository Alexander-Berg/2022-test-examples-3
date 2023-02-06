#!/usr/bin/env python
# -*- coding: utf-8 -*-


import tempfile
import os

import yatest.common


def get_model_predictions(config_path):
    output_file = tempfile.NamedTemporaryFile(delete=False).name
    yatest.common.execute([
        yatest.common.binary_path('cv/imgclassifiers/danet/external/ext_nn_runner/ext_nn_runner'),
        '--mode', 'predict',
        '--config_path', yatest.common.data_path(config_path),
        '--images_dir', os.readlink('test_images'),  # I have no idea why ext_nn_runner doesn't work with symlinks
        '--output_file', output_file])
    predictions = open(output_file).readlines()
    assert len(predictions) == 1

    predictions = predictions[0].split()
    assert predictions[0].endswith('a.jpg')

    return dict(zip(predictions[1::2], predictions[2::2]))


def verify_predictions(predictions_left, predictions_right, left_only_labels, right_only_labels):
    assert all([label in predictions_left for label in left_only_labels])
    assert all([label in predictions_right for label in right_only_labels])
    assert set(predictions_left.keys()) - left_only_labels == set(predictions_right.keys()) - right_only_labels
    for key in predictions_left:
        if key not in left_only_labels:
            assert predictions_left[key] == predictions_right[key]


def get_model_features(config_path, layer_names):
    result = {}
    output_file = tempfile.NamedTemporaryFile(delete=False).name
    for layer_name in layer_names:
        yatest.common.execute([
            yatest.common.binary_path('cv/imgclassifiers/danet/external/ext_nn_runner/ext_nn_runner'),
            '--mode', 'save_features',
            '--layer_name', layer_name,
            '--config_path', yatest.common.data_path(config_path),
            '--images_dir', os.readlink('test_images'),
            '--output_file', output_file])
        features = open(output_file).readlines()
        assert len(features) == 1
        features = features[0].rstrip().split('\t')
        assert features[0].endswith('a.jpg')
        result[layer_name] = map(float, features[1:])
    return result


def verify_features(left, right):
    assert set(left.keys()) == set(right.keys())
    for layer_name, features_left in left.iteritems():
        features_right = right[layer_name]
        assert(len(features_left) == len(features_right))
        assert all([features_left[i] == features_right[i] for i in range(len(features_left))])


def test_multihead_net_ver5_preds():
    trigger_only_labels = set()
    cbir_daemon_only_labels = {'auto_aircraft', 'auto_car', 'auto_not_vehicle', 'auto_other_vehicle'}

    predictions_trigger = get_model_predictions('images/multihead_net_ver5/multihead_net_ver5.cfg')
    predictions_cbir_daemon = get_model_predictions('images/multihead_net_ver5/cbir_daemon/multihead_net_ver5.cfg')

    verify_predictions(predictions_trigger, predictions_cbir_daemon, trigger_only_labels, cbir_daemon_only_labels)


def test_multihead_net_ver5_features():
    layer_names = ['prod_v5_enc_toloka_96', 'prod_v5_enc_i2t_v7_200_img']

    features_trigger = get_model_features('images/multihead_net_ver5/multihead_net_ver5.cfg', layer_names)
    features_daemon = get_model_features('images/multihead_net_ver5/cbir_daemon/multihead_net_ver5.cfg', layer_names)

    verify_features(features_trigger, features_daemon)


def test_multihead_net_ver6_preds():
    trigger_only_labels = set()
    cbir_daemon_only_labels = set()

    predictions_trigger = get_model_predictions('images/multihead_net_ver6/multihead_net_ver6.cfg')
    predictions_cbir_daemon = get_model_predictions('images/multihead_net_ver6/cbir_daemon/multihead_net_ver6.cfg')

    verify_predictions(predictions_trigger, predictions_cbir_daemon, trigger_only_labels, cbir_daemon_only_labels)


def test_multihead_net_ver6_features():
    layer_names = ['prod_v6_enc_toloka_96', 'prod_v6_enc_i2t_v8_200_img']

    features_trigger = get_model_features('images/multihead_net_ver6/multihead_net_ver6.cfg', layer_names)
    features_daemon = get_model_features('images/multihead_net_ver6/cbir_daemon/multihead_net_ver6.cfg', layer_names)

    verify_features(features_trigger, features_daemon)
