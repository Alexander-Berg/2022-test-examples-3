from sprav.protos import company_pb2
from sprav.protos import export_pb2
import yt.wrapper as yt
from yt.wrapper.ypath import ypath_join
import google.protobuf.text_format as text_format
import os


def read_pb_txt(test_data_dir, name, klass):
    path = os.path.join(test_data_dir, '{}.pb.txt'.format(name))
    data = []
    with open(path, 'r') as infile:
        for line in infile:
            if line.startswith('----------'):
                yield text_format.Parse(''.join(data), klass())
                data = []
            else:
                data.append(line)


def upload_companies_data(yt_client, table, test_data_path):
    '''
    Args:
        yt_client (yt.wrapper.YtClient)
        table (str): destination path on YT, e. g. '//home/sprav/altay/prod/snapshot/company'
        test_data_path (str): local directory which contains source *.pb.txt files
    '''

    # source_proto's are stored in a separate file
    tds_companies = {
        message.export_id: message.SerializeToString()
        for message in read_pb_txt(test_data_path, 'tds_company', company_pb2.Company)
    }

    rows = [
        {
            'permalink': message.Id,
            'source_proto': tds_companies.get(message.Id),
            'export_proto': message.SerializeToString(),
            'is_exported': True,
            'publishing_status': 'publish',
        }
        for message in read_pb_txt(test_data_path, 'company', export_pb2.TExportedCompany)
    ]

    rows.sort(key=lambda row: row['permalink'])
    yt_client.write_table(yt.TablePath(table, sorted_by=['permalink']), rows)


def upload_annotations(yt_client, ann_path, destination_path):
    data = []
    with open(ann_path, 'r') as f:
        for line in f:
            items = line.split('\t')
            data.append({'key': items[0], 'value': items[1]})
    yt_client.write_table(destination_path, (x for x in data), format='yson')


def upload_exported_dir(yt_client, exported_path, test_data_path):
    '''
    Args:
        yt_client (yt.wrapper.YtClient)
        exported_path (str): destination path on YT, e. g. '//home/altay/db/export/current-state/exported'
        test_data_path (str): local directory which contains source *.pb.txt files
    '''

    def _upload(entity, klass):
        rows = [
            {
                'id': message.Id,
                'exported_{}'.format(entity): message.SerializeToString(),
            }
            for message in read_pb_txt(test_data_path, entity, klass)
        ]
        yt_client.write_table(ypath_join(exported_path, entity), rows)

    _upload('rubric', export_pb2.TExportedRubric)
    _upload('chain', export_pb2.TExportedChain)
    _upload('provider', export_pb2.TExportedProvider)
    _upload('feature', export_pb2.TExportedFeature)
    _upload('fast_feature', export_pb2.TExportedFastFeature)
