import subprocess
import logging
from time import sleep


def init():
    def run(cmd):
        logging.info('pulse_init: [{}] code {} '.format(' '.join(cmd), subprocess.call(cmd)))
    run(['pulseaudio', '-k'])
    run(['pulseaudio', '--start'])
