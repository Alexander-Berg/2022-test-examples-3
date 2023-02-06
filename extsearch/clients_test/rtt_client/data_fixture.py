import pytest

import extsearch.video.robot.rt_transcoder.proto.task_pb2 as task_pb2
from google.protobuf.json_format import MessageToDict


@pytest.fixture(scope='session')
def test_task_id():
    return '8e8ae4d3-8b63-4a6d-9d8d-f0746f545803'


@pytest.fixture(scope='session')
def tasks(test_task_id):
    return [
        {'task_id': test_task_id},
        {'task_id': '99de4a63-a04e-45c9-9fb8-2b640fd0c26e'},
        {'task_id': '9d133f0a-14da-40aa-b6e9-400b86305f63'},
    ]


@pytest.fixture(scope='session')
def video_id():
    return {
        0: '1234567891233',
        2: '123456123123'
    }


@pytest.fixture
def new_task_ok(video_id):
    task = task_pb2.TTask()
    task.InputVideoUrl = 'http://test.ru/video.mp4'
    task.PreviewS3Params.KeyPrefix = '123456789102022'
    task.ExternalID = video_id.get(0)
    return task


@pytest.fixture
def new_task_data_ok(new_task_ok):
    return MessageToDict(new_task_ok)


@pytest.fixture
def new_task_err_2(video_id):
    task = task_pb2.TTask()
    task.InputVideoUrl = 'http://test.ru/video.mp4'
    task.PreviewS3Params.KeyPrefix = '123456789102022'
    task.ExternalID = video_id.get(2)
    return MessageToDict(task)
