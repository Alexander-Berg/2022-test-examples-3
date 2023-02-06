from aiohttp import web
import logging
import argparse
import datetime
import requests
import subprocess
import os
import sys


def arg_init():
    global PORT
    global DEBUG_PARAM

    parser = argparse.ArgumentParser()
    parser.add_argument('--port', action='store', type=int, help='application port', default=9000)
    args, unknown = parser.parse_known_args()

    if 60000 > args.port > 1024:
        PORT = args.port
    else:
        logging.error('Wrong port, use range(1024-60000), set to default 9000')
        PORT = 9000
    return PORT

def testfunc():
    return os.getloadavg()


async def test(request):
    return web.Response(text=f'<html><head><meta http-equiv="Refresh" content="1"></meta></head><p>{subprocess.getoutput('hostname')}</p></html>', content_type='text/html')


app = web.Application()
app.add_routes([web.get('/test', test)])


def main():
    arg_init()
    logging.basicConfig(level=logging.DEBUG, filename='app.log')
    web.run_app(app=app, host='::', port=PORT, print=logging.info)
