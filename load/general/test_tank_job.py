
from __future__ import print_function
import logging

import grpc

import tank_job_pb2
import tank_job_pb2_grpc


def find_file_path(file_name):
    from pathlib import Path, PurePath
    return PurePath().joinpath(
        Path().resolve(),
        'test_data',
        file_name
    )


def read_in_chunks(file_path, file_type, tank_job='11532', chunk_size=1024):
    with open(file_path, 'rb') as input_file:
        while True:
            data = input_file.read(chunk_size)
            if not data:
                break
            message = tank_job_pb2.Chunk(
                chunk_content=data,
                file_type=file_type,
                tank_job=tank_job
            )
            yield message



def run():
    with grpc.insecure_channel('localhost:50051') as channel:
        stub = tank_job_pb2_grpc.TankJobServiceStub(channel)
        response = stub.UploadFile(
            read_in_chunks(
                find_file_path('test_config_01.yaml'),
                # find_file_path('test_ammo_01.ammo'),
                file_type=0
            )
        )
        if response.upload_status == 1:
            status = 'OK'
        elif response.upload_status == 2:
            status = 'FAILED'
        else:
            status = 'UNKNOWN'
        print('Chunk sent, status ' + status)


if __name__ == '__main__':
    logging.basicConfig()
    run()
