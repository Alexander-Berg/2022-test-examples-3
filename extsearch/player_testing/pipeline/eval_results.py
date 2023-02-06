#!/usr/bin/env python
# coding: utf-8
import json
import argparse
import logging
import host_status
from config import Config
from host_status import HostStatus
from util import process_samples
import codecs


def get_status(config, metrics, host):
    if metrics['loss_rate'] > config.max_loss_rate:
        return HostStatus.INCOMPLETE_CHECK
    if metrics['is_html'] and metrics['is_video']:
        return HostStatus.MIXED_MIME
    elif not metrics['is_html'] and not metrics['is_video']:
        return HostStatus.HTTP_NOT_FOUND
    if metrics['vdp_rate'] > config.max_vdp_rate:
        return HostStatus.VDP
    if metrics['scrolling_rate'] > config.max_scrolling_rate:
        return HostStatus.SCROLLING
    if metrics['known_rate'] > config.max_known_player_rate:
        return HostStatus.MIRROR
    if metrics['popup_rate'] > config.max_popup_rate:
        return HostStatus.POPUP
    if metrics['moving_area'] < config.min_moving_area:
        return HostStatus.NOT_MOVING
    if host in config.banned_hosts:
        return HostStatus.BANNED
    if metrics['is_html']:
        return HostStatus.SEARCHABLE_HTML
    else:
        return HostStatus.SEARCHABLE_VIDEO


def calc_metrics(total, lost, html, video, popup, scrolling, known, play_area_start, play_area_max):
    checked = max(0, total - lost)
    playing = html + video
    play_area_start.sort()
    play_area_max.sort()
    return {
        'loss_rate': min(1.0, float(lost) / total) if total else 1.0,
        'popup_rate': min(1.0, float(popup) / checked) if checked else 0.0,
        'scrolling_rate': min(1.0, float(scrolling) / checked) if checked else 0.0,
        'known_rate': min(1.0, float(known) / checked) if checked else 0.0,
        'vdp_rate': max(float(checked - playing) / checked, 0.0) if checked else 1.0,
        'video_rate': float(video) / checked if checked else 0.0,
        'html_rate': float(html) / checked if checked else 0.0,
        'is_video': video > 0,
        'is_html': html > 0,
        'sample_size': total,
        'moving_area': 0.0 if len(play_area_max) == 0 else play_area_max[len(play_area_max) / 2],
        'autoplay_area': 0.0 if len(play_area_start) == 0 else play_area_start[len(play_area_start) / 2]
    }


def eval_results(host_item, config, url_status):
    total = 0
    lost = 0
    html = 0
    video = 0
    popup = 0
    known = 0
    scrolling = 0
    play_area_start = []
    play_area_max = []

    for url in host_item['Sample']:
        total += 1
        if url not in url_status:
            lost += 1
            continue
        status = url_status[url]
        if status['is_playing']:
            if status['is_video']:
                video += 1
            elif status['is_html']:
                html += 1
            play_area_start.append(status.get('play_area1', 0.0))
            play_area_max.append(max(status.get('play_area1', 0.0), status.get('play_area2', 0.0)))
        elif status['is_popup']:
            popup += 1
        scrolling += status.get('is_scrolling', 0)
        known += status.get('has_known_player', 0)
    metrics = calc_metrics(total, lost, html, video, popup, scrolling, known, play_area_start, play_area_max)
    host_item['Status'] = get_status(config, metrics, host_item['Host'])
    metrics.update(host_item.get('Metrics', {}))
    metrics['version'] = config.version
    host_item['Metrics']  = metrics
    host_item['Autoplay'] = host_item['Status'] == HostStatus.SEARCHABLE_HTML and metrics['autoplay_area'] >= config.min_moving_area


def load_url_status(fd):
    url_status = {}
    for item in json.load(fd):
        url_status[item['url']] = item
    return url_status


if __name__ == '__main__':
    logging.basicConfig(level=logging.INFO)
    config = Config()
    ap = argparse.ArgumentParser()
    ap.add_argument('--hosts', required=True, type=argparse.FileType('r'))
    ap.add_argument('--url-status', required=True, type=argparse.FileType('r'))
    ap.add_argument('--output', required=True, type=argparse.FileType('w'))
    args = ap.parse_args()
    url_status = load_url_status(args.url_status)
    hosts = json.load(args.hosts)
    process_samples(hosts, eval_results, config=config, url_status=url_status)
    json.dump(hosts, args.output, indent=1, sort_keys=True)
