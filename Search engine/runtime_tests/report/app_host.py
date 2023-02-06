import lz4.block
import struct
import json
import zstandard as zstd
from report.proto import service_pb2
import yt.yson

from runtime_tests.util.proto.http import message as mod_msg
from runtime_tests.util.proto.handler.server.http import StaticResponseHandler

def decode(data):
    code = data[0:2]
    if code == 'y_':
        result = yt.yson.yson_to_json(yt.yson.loads(data[2:]))
    else:
        result = json.loads(data)

    result.pop('__compressed__', None)

    return result

def encode(data):
    return json.dumps(data)

def uncompress(data):
    (codec,) = struct.unpack('B', data[0:1])
    (length,) = struct.unpack('Q', data[1:9])
    if codec == 2:
        return lz4.block.decompress(data[9:], uncompressed_size=length)
    elif codec == 5:
        ctx = zstd.ZstdDecompressor()
        return ctx.decompress(data[9:], max_output_size=length)
    elif codec == 0:
        return data[1:]
    else:
        raise ValueError("Unknown codec: {}".format(codec))

def compress(data):
    codec = struct.pack('B', 2)
    length = struct.pack('Q', len(data))
    return codec + length + lz4.block.compress(data, store_size=False)

def convert_request_proto(content):
    d = service_pb2.TServiceRequest()
    d.ParseFromString(content)

    sources = []
    source_by_name = {}

    for answer in d.Answers:
        uncompressed = uncompress(answer.Data)
        if uncompressed[0:2] == "p_":
            continue

        result = decode(uncompressed)

        if answer.SourceName in source_by_name:
            source = source_by_name[answer.SourceName]
        else:
            source = { 'name': answer.SourceName, 'results': [] }
            source_by_name[answer.SourceName] = source
            sources.append(source)

        result['type'] = answer.Type
        source['results'].append(result)

    return json.dumps(sources)

def convert_response_proto(data):
    sources = json.loads(data)
    r = service_pb2.TServiceResponse()

    for source in sources:
        for result in source['results']:
            answer = r.Answers.add()
            answer.SourceName = source['name'].encode('utf-8')
            if 'type' in result:
                answer.Type = result['type'].encode('utf-8')
            answer.Data = compress(encode(result))

    return r.SerializeToString()


class AppHostHTTPHandler(StaticResponseHandler):
    def handle_parsed_request(self, raw_request, stream):
        path = raw_request.request_line.path
        raw_resp = self.config.response
        if not ('/_json/' in path or '/_yson/' in path):
            resp = raw_resp.to_response()
            try:
                new_data = convert_response_proto(resp.data.content)
            except Exception, e:
                raise Exception("Failed to convert {} (content={}): {}".format(path, resp.data.content, e))
            raw_resp = mod_msg.HTTPResponse(resp.status_line, resp.headers, new_data).to_raw_response()

        stream.write_response(raw_resp)
        self.finish_response()
