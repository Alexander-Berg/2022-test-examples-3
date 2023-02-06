# coding: utf-8

from ads.bsyeti.big_rt.py_test_lib import (
    extract_all_states_from_simple_table,
)
from market.idx.datacamp.picrobot.processor.proto.state_pb2 import (
    TPicrobotState
)

from .conftest import (
    pack_message,
    request_event,
    launch,
    image_event,
    generate_mds_json,
    ext_meta_update_event,
)


def test_exp_meta_update(stand):
    data = {0: [
        pack_message(request_event(
            url='https://example.com/new_img1',
            namespace='marketpic',
            offer='https://example.com/offer'
        )),
        pack_message(image_event(
            url='https://example.com/new_img1',
            mds_json=generate_mds_json(
                namespace='marketpic'
            ),
            quant_local_descriptors='qld1'.encode('utf-8')
        )),
    ]}
    stand.input_yt_queue["queue"].write(data)
    launch(stand, data)

    state = extract_all_states_from_simple_table(stand.yt_client, stand.state_table_path, decompress=True)

    img1_state = TPicrobotState()
    img1_state.ParseFromString(state['https://example.com/new_img1'])

    assert len(img1_state.MdsInfo) == 1
    assert img1_state.MdsInfo[0].MdsId.Namespace == 'marketpic'
    assert img1_state.MdsInfo[0].MdsId.ImageName == 'pic123'
    img_props = img1_state.ImageMetaData
    assert img_props.ExtImageAttributes.Colorness == 0.0
    assert img_props.QuantizedLocalDescriptors == 'qld1'.encode('utf-8')

    # проверяем что работает update
    data = {0: [
        pack_message(ext_meta_update_event(
            url='https://example.com/new_img1',
            colorness=1.0,
            key=0,
            featureIdx=1,
            quant_local_descriptors='qld2'.encode('utf-8')
        )),
        pack_message(ext_meta_update_event(
            url='https://example.com/new_img1',
            key=1,
            featureIdx=2,
        ))
    ]}
    stand.input_yt_queue["queue"].write(data)
    launch(stand, data)

    state = extract_all_states_from_simple_table(stand.yt_client, stand.state_table_path, decompress=True)
    img1_state.ParseFromString(state['https://example.com/new_img1'])

    assert len(img1_state.MdsInfo) == 1
    assert img1_state.MdsInfo[0].MdsId.Namespace == 'marketpic'
    assert img1_state.MdsInfo[0].MdsId.ImageName == 'pic123'
    img_props = img1_state.ImageMetaData
    assert img_props.ExtImageAttributes.Colorness == 1.0
    assert img_props.QuantizedLocalDescriptors == 'qld2'.encode('utf-8')
    assert len(img_props.NeuralNetOutputs[0].NeuralNetOutputs.Features) == 1
    assert len(img_props.NeuralNetOutputs[1].NeuralNetOutputs.Features) == 1
    assert img_props.NeuralNetOutputs[0].NeuralNetOutputs.Features[0].LayerIdx == 1
    assert img_props.NeuralNetOutputs[1].NeuralNetOutputs.Features[0].LayerIdx == 2

    # проверяем что работает удаление
    data = {0: [
        pack_message(ext_meta_update_event(
            url='https://example.com/new_img1',
            colorness=2.0,
            featureIdx=3,
            key=0,
            deleted=True
        ))
    ]}
    stand.input_yt_queue["queue"].write(data)
    launch(stand, data)

    state = extract_all_states_from_simple_table(stand.yt_client, stand.state_table_path, decompress=True)
    img1_state.ParseFromString(state['https://example.com/new_img1'])

    assert len(img1_state.MdsInfo) == 1
    img_props = img1_state.ImageMetaData
    assert img_props.ExtImageAttributes.Colorness == 2.0
    assert img_props.NeuralNetOutputs[0].Deleted is True
    assert img_props.NeuralNetOutputs[1].NeuralNetOutputs.Features[0].LayerIdx == 2
