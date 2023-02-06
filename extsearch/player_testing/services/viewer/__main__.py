from extsearch.video.robot.crawling.player_testing.protos.job_pb2 import TJob, EArtifactType
from flask import Flask, render_template, request
from google.protobuf.json_format import MessageToJson
from conf import Config, SANDBOX_URL
from hashlib import md5
from sqs import SQSClient
from job import verify_job
from log_aggregator import LogAggregatorClient
import argparse
import urllib
import os


app = Flask(__name__, template_folder=os.path.join(os.getcwd(), 'templates'))


@app.route('/')
def index():
    return render_template('index.html')


def add_https(url):
    if url.startswith('https://'):
        return url
    elif url.startswith('http://'):
        return 'https://{}'.format(url[7:])
    else:
        return 'https://{}'.format(url)


def wrap_up_player(url, autodetect, add_yastatic, add_sandbox):
    if url is None or len(url) == 0:
        raise Exception('empty URL')
    if autodetect:
        return url
    url = add_https(url)
    if url.startswith('https://yastatic.net'):
        return url
    if not add_yastatic:
        return url
    sandbox = ' sandbox="yes"'.format() if add_sandbox else ''
    player_code = '<iframe src="{}"{} allow="autoplay; fullscreen"></iframe>'.format(url, sandbox)
    return '{}#html={}'.format(SANDBOX_URL, urllib.quote(player_code))


@app.route('/check', methods=['POST'])
def check():
    config = Config()
    url = None
    autodetect = False
    try:
        add_yastatic = request.form.get('yastatic', 'net') == 'da'
        add_sandbox = request.form.get('sndbox', 'net') == 'da'
        autodetect = request.form.get('autodetect', 'net') == 'da'
        videorec = int(request.form.get('videorec', 0))
        url = wrap_up_player(request.form.get('url'), autodetect, add_yastatic, add_sandbox)
    except Exception as e:
        return render_template('error.html', error=str(e))
    job = TJob()
    job.Url = url
    job.Id = md5(url).hexdigest()
    job.Flags.HttpProbe = autodetect
    job.Browser = request.form.get('browser')
    job.Device = request.form.get('device')
    if videorec:
        job.VideoCapture.Duration = videorec
    verify_job(job)
    SQSClient(config.sqs).get_queue(config.sqs.input_queues.keys()[0]).push(job.SerializeToString())
    return render_template('wait.html', job_id=job.Id)


def get_job_result(logsvc, job_id):
    res = logsvc.url_get(job_id)
    return res if res.Job.Id == job_id else None


@app.route('/status')
def status():
    job_id = request.args.get('job_id')
    config = Config()
    logsvc = LogAggregatorClient(config.logsvc, config.http.proxy_port)
    return '1' if get_job_result(logsvc, job_id) else '0'


@app.route('/results')
def results():
    job_id = request.args.get('job_id')
    config = Config()
    logsvc = LogAggregatorClient(config.logsvc, config.http.proxy_port)
    res = get_job_result(logsvc, job_id)
    if not res:
        return render_template('error.html', error='Job {} not found'.format(job_id))
    else:
        params = [{'key': '', 'value': MessageToJson(res)}]
        img = []
        videos = []
        for item in res.Artifacts:
            if item.Type == EArtifactType.EAT_VIDEO:
                videos.append({'url': item.Url})
            elif item.Type == EArtifactType.EAT_IMAGE:
                img.append({'url': item.Url})
        finished = res.Finished != 0
        return render_template('results.html', params=params, finished=finished, img=img, videos=videos)


if __name__ == '__main__':
    ap = argparse.ArgumentParser()
    ap.add_argument('--port', default=8093, type=int)
    args = ap.parse_args()
    os.environ['SNAIL_ENV'] = 'viewer'
    config = Config()
    app.run(host='::', port=args.port)
