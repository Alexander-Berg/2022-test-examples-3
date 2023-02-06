from extsearch.video.robot.crawling.player_testing.protos.job_pb2 import TJobResult, THttpResource, EVideoFormat
from flask import Flask, Response, request, current_app, jsonify
from conf import Config
from player_test import is_playing
from util import init_root_logger
from waitress import serve
from time import time
import os


context = None
app = Flask(__name__)
app.config['DEBUG'] = False


def current_job():
    if not hasattr(current_app, 'current'):
        current_app.current = TJobResult()
    return current_app.current


def result_cache():
    if not hasattr(current_app, 'finished'):
        current_app.finished = []
    return current_app.finished


@app.route('/job/start', methods=['POST'])
def job_start():
    current_app.current = TJobResult()
    current_app.current.ParseFromString(request.data)
    current_app.current.Started = int(time())
    return jsonify(status='success')


@app.route('/job/update', methods=['POST'])
def job_update():
    current_job().MergeFromString(request.data)
    return jsonify(status='success')


def make_response(job):
    return Response(job.SerializeToString(),
                    headers={'content-type': 'application/octet-stream'},
                    status=200)


@app.route('/job/finish')
def job_finish():
    job = current_job()
    job.IsPlaying = is_playing(job)
    job.Finished = int(time())
    if os.environ.get('SNAIL_ENV') == 'viewer':
        result_cache().append(job)
    return make_response(job)


@app.route('/job/get')
def job_get():
    jobid = request.args.get('job_id')
    resp = TJobResult()
    resp.Job.Id = jobid
    if current_job().Job.Id == jobid:
        resp.CopyFrom(current_job())
    else:
        for item in result_cache():
            if item.Job.Id == jobid:
                resp.CopyFrom(item)
    return make_response(resp)


def is_content_link(logitem):
    return logitem.ContentType.find('video') != -1 or logitem.ContentType.find('audio') != -1 or logitem.VideoFormat != EVideoFormat.EVF_UNKNOWN


def is_pretty_video(content):
    return (content.ContentType.find('video') != -1 or content.VideoFormat != EVideoFormat.EVF_UNKNOWN) and content.ContentSize > 100000


KNOWN_PLAYER = {
    'www.youtube.com/embed/': 'youtube',
    'vk.com/video_ext.php': 'vk',
    'ok.ru/videoembed/': 'ok',
    'ppembed.com/embed/': 'prostoporno',
    'xvideos.com/embedframe': 'xvideos',
    'www.dailymotion.com/embed/video/': 'dailymotion'
}


def parse_player(logitem):
    if logitem.ContentType.find('text/html') == -1:
        return None
    for pattern in KNOWN_PLAYER.iterkeys():
        if logitem.Url.find(pattern) != -1:
            return KNOWN_PLAYER[pattern]
    return None


@app.route('/http/log', methods=['POST'])
def http_log():
    logitem = THttpResource()
    logitem.ParseFromString(request.data)
    if is_content_link(logitem):
        content = current_job().Http.Contents.add()
        content.CopyFrom(logitem)
        if is_pretty_video(content):
            current_job().Http.PrettyVideoContent = True
    player_id = parse_player(logitem)
    if player_id is not None:
        player = current_job().Player.Known.add()
        player.PlayerId = player_id
        player.EmbedUrl = logitem.Url
    return jsonify(status='success')


@app.route('/http/pretty_video')
def http_pretty_video():
    return jsonify(has_pretty_video=current_job().Http.PrettyVideoContent)


@app.route('/http/known_player')
def http_known_player():
    return jsonify(has_known_player=(len(current_job().Player.Known) != 0))


@app.route('/ping')
def pint():
    return jsonify(status='success')


if __name__ == '__main__':
    init_root_logger()
    config = Config()
    serve(app, host='::1', port=config.logsvc.server_port, threads=1)
