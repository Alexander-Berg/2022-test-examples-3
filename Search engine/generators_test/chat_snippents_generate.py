#  -*- coding: utf-8 -*-
import os
import sys
import cgi
import json
import argparse
import yt.wrapper as yt
from yt.wrapper.common import GB

sys.path.append(os.path.join(os.path.dirname(__file__), '..', 'system'))
import misc

snippet_name = 'chat_xml/1.x'


def escaped(src):
    result = {}
    for k, v in src.iteritems():
        if v is None:
            v = u''
        if isinstance(v, str):
            v = v.decode('utf-8')
        if isinstance(v, unicode):
            v = cgi.escape(v)
        result[k] = v
    return result


@yt.with_context
def reduce_with_altay(key, rows, context):
    snippet_row = None
    permalinks = []
    for row in rows:
        if row['@table_index'] == 0:
            snippet_row = row
        else:
            permalinks.append(row['permalink'])
    for permalink in permalinks:
        if snippet_row is not None:
            value = escaped(snippet_row['value'])
            snippet = (u'{snippet_name}='
                       u'<ChatSnippet>'
                            u'<orgName>{org_name}</orgName>'
                            u'<socketUrl>{socket_url}</socketUrl>'
                            u'<orgId>{org_id}</orgId>'
                            u'<title>{title}</title>'
                            u'<wait_time>{wait_time}</wait_time>'
                            u'<bot_id>{bot_id}</bot_id>'
                       u'</ChatSnippet>').format(snippet_name=snippet_name,
                                                 org_name=value['org_name'],
                                                 socket_url=value['socket_url'],
                                                 org_id=value['org_id'],
                                                 title=value['title'],
                                                 wait_time=value['wait_time'],
                                                 bot_id=value['bot_id'])
            snippet = snippet.replace('\n', '')
            yield {'@table_index': 0,
                   'key': str(permalink),
                   'value': snippet}
    if len(permalinks) == 0:
        snippet_row['@table_index'] = 1
        yield snippet_row


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Chat snippets mapper')
    parser.add_argument('--cluster', type=str, help='YT cluster')
    parser.add_argument('--parameters', type=str,
                        help='Dict with job parameters')
    args = parser.parse_args()
    params = json.loads(args.parameters)
    yt_client = misc.get_client(os.environ['YT_TOKEN'], args.cluster)
    yt_client.run_sort(params.get('pre_processing_out'),
                       sort_by=params.get('key_type'))
    yt_client.run_sort(misc.ALTAY_TABLE, sort_by=params.get('key_type'))
    yt_client.run_reduce(reduce_with_altay,
                         [params.get('pre_processing_out'), misc.ALTAY_TABLE],
                         [params.get('generating_out') or params.get('processing_out'),
                         '//tmp/geosearch_chat_xml_generation_err'],
                         format=yt.JsonFormat(control_attributes_mode='row_fields'),
                         memory_limit=5*GB,
                         reduce_by=params.get('key_type'))
