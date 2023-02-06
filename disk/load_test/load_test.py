import random
import re
import threading
import json
import pycurl
import time

from categorization import categorize_response
from http_request import request_http
from stats import StatEntity
from multithreaded_runner import run_multithreaded
from tabulate import tabulate
from stidhelper import get_url_by_stid


class Params:
    def __init__(self):
        self.host = 'http://disk-streaming-int.ape.yandex.net'
        self.show_ok = False
        self.thread_count = 1
        self.url_providers = None
        self.long_request_threshold = 25
        self.ffmpeg_in_threads = 0
        self.ffmpeg_out_threads = 4
        self.ffmpeg_max_proc_count = 11
        self.init_prefetch_size = 2
        self.segment_duration = 5
        self.disable_cache = False
        self.disable_dynamic_prefetch = False
        self.stids = None
        self.test_duration = 60 * 5
        self.repeat_ratio = 0

    @property
    def qs_params(self):
        return '&' + '&'.join([
            'disable_dynamic_prefetch=%s' % str(self.disable_dynamic_prefetch),
            'disable_cache=%s' % str(self.disable_cache),
            'suppress_output=true',
            'ffmpeg_in_threads=%d' % self.ffmpeg_in_threads,
            'ffmpeg_out_threads=%d' % self.ffmpeg_out_threads,
            'ffmpeg_cores=%d' % self.ffmpeg_out_threads,
            'max_proc_count=%d' % self.ffmpeg_max_proc_count,
            'init_prefetch_size=%d' % self.init_prefetch_size,
            'enable_wait_transcoding=false',
            'segment_duration=%d' % self.segment_duration,
            'max_cpu_load=%.2f' % 7.5,
            'left_over_duration=%d' % 0
        ])

    def log(self, value=''):
        print value

    def log_short(self, value=''):
        print value

    def set_stid_file(self, arg):
        with open(arg) as f:
            stids = f.readlines()
            stids = [s.strip() for s in stids]
            self.set_stids(stids)

    def _get_url_by_stid_lambda(self, stid):
        return lambda: get_url_by_stid(self.host, stid, self.qs_params)

    def set_stid(self, stid):
        self.set_stids([stid])

    def set_stids(self, stids):
        self.url_providers = [self._get_url_by_stid_lambda(stid) for stid in stids]

class Stats:
    def __init__(self, params):
        self.params = params
        self._lock = threading.Lock()
        self._stats = StatEntity()

    def rec_playlist_await(self, await_time, dimension):
        with self._lock:
            self._stats.playlist.await.all.append(await_time)
            self._stats.playlist.dimension[dimension].all.increment()
            if await_time > 0:
                self._stats.playlist.await.nonzero.append(await_time)
                self._stats.playlist.dimension[dimension].nonzero_await.increment()

    def rec_segment(self, playlist_req, seg_req, seg_index, seg_duration, seg_await, non_app_segment_time):
        with self._lock:
            self._stats.segment.request[str(seg_req.status) + ' ' + seg_req.category].timings.append(seg_req.duration)
            self._stats.segment.total_duration.increment(seg_duration)
            if seg_req.status == 200:
                if non_app_segment_time is not None:
                    self._stats.segment.non_app_time.append(seg_req.duration - non_app_segment_time)

                if seg_index == 0:
                    self._stats.start.first_segment_plus_playlist.append(playlist_req.total_duration + seg_req.total_duration)
                    self._stats.start.first_segment.append(seg_req.total_duration)
                else:
                    self._stats.segment.await.all.append(seg_await)
                    if seg_await > 0:
                        self._stats.segment.await.nonzero.append(seg_await)

    def rec_load_average(self, load_average, source):
        with self._lock:
            self._stats.load_average.append(load_average)

    def total_count(self):
        return sum([row.timings.count() for row in self._stats.segment.request.rows()])

    def error_count(self):
        return self.total_count() - self.ok_count()

    def ok_count(self):
        return self._stats.segment.request['200 OK'].timings.count()

    def print_stats(self):
        error_ratio = 100.0 * self.error_count() / self.total_count() if self.total_count() > 0 else 0.0

        self.log()
        self.log('STATS')
        self.log('*' * 20)

        self.log()
        self.log('Request results')
        self.log(tabulate(
            self._stats.segment.request.get_values(
                lambda row: row.name,
                lambda row: row.timings.count(),
                lambda row: row.timings.rps(self.params.test_duration),
                sort_by=(lambda row: row.timings.count)
            ),
            headers=['name', 'count', 'RPS'],
            floatfmt='.2f'
        ))

        self.log()
        self.log('Videoclip dimensions')
        self.log(tabulate(
            self._stats.playlist.dimension.get_values(
                lambda row: row.name,
                lambda row: 100.0 * int(row.nonzero_await) / int(row.all),
                lambda row: int(row.nonzero_await),
                lambda row: int(row.all),
                sort_by=(lambda row: int(row.all))
            ),
            headers=['name', 'freeze %', 'freeze count', 'total count'],
            floatfmt='.2f',
            tablefmt='grid'
        ))

        self.log()
        self.log('error/total ratio: %.2f%%' % error_ratio)

        self.log()
        self.log('Non-app request timings')
        self._print_percentiles(self._stats.segment.non_app_time, 50, 75, 90, 95, 97, 98, 99)

        self.log()
        self.log('Request timings')
        self._print_percentiles(self._stats.segment.request['200 OK'].timings, 50, 75, 90, 95, 97, 98, 99)

        self.log()
        self.log('Start await timings')
        self._print_percentiles_multi(
            [
                ('playlist + first segment', self._stats.start.first_segment_plus_playlist),
                ('first segment', self._stats.start.first_segment)
            ],
            50, 75, 90, 95, 97, 98, 99, 99.99
        )

        self.log()
        self.log('Segment await timings')
        self._print_nonzero_and_all_centiles(
            self._stats.segment.await.nonzero,
            self._stats.segment.await.all,
            50, 75, 90, 95, 97, 98, 99, 99.99
        )

        self.log()
        self.log('Videoclip total freeze timings')
        self._print_nonzero_and_all_centiles(
            self._stats.playlist.await.nonzero,
            self._stats.playlist.await.all,
            50, 75, 90, 95, 97, 98, 99, 99.99
        )

        self.log()
        self.log('Test params')
        self.log_short(tabulate(
            [['%d sec' % self.params.segment_duration, self.params.init_prefetch_size,
              not self.params.disable_dynamic_prefetch,
              '%d / %d' % (self.params.ffmpeg_in_threads, self.params.ffmpeg_out_threads),
              self.params.ffmpeg_max_proc_count,
             self.params.thread_count, self.params.test_duration]],
            headers=['segment', 'prefetch', 'dynamic prefetch', 'ff in/out threads', 'ff max proc', 'concurrency',
                     'test time'],
            tablefmt='grid'
        ))

        total_segment_duration_per_sec = 1.0 * int(self._stats.segment.total_duration) / self.params.test_duration
        total_segment_duration_per_sec_per_core = total_segment_duration_per_sec / self._stats.load_average.median()
        self.log_short()
        self.log_short(tabulate(
            [
                ('Median load average', '%.2f' % self._stats.load_average.median()),
                ('Transcode speed (secs of video per sec)', '%.2f' % total_segment_duration_per_sec),
                ('Transcode speed per LA=1.0', '%.2f' % total_segment_duration_per_sec_per_core),
                ('Segment freeze', Stats._format_out_of(len(self._stats.segment.await.nonzero), len(self._stats.segment.await.all))),
                ('Playlist freeze', Stats._format_out_of(len(self._stats.playlist.await.nonzero), len(self._stats.playlist.await.all))),
                ('Start: playlist + first segment, sec', Stats._format_percentile_range(self._stats.start.first_segment_plus_playlist)),
                ('Start: first segment, sec', Stats._format_percentile_range(self._stats.start.first_segment)),
            ]
        ))
        self.log_short()
        self.log_short()

    def log(self, value=''):
        self.params.log(value)

    def log_short(self, value=''):
        self.params.log_short(value)

    @staticmethod
    def _format_out_of(portion, all):
        return '%.2f%% (%d out of %d)' % (100.0 * portion / all, portion, all)

    @staticmethod
    def _format_percentile_range(source):
        return '%.2f - %.2f  (50%% - 99%%)' % (source.percentile(50), source.percentile(99))

    def _print_nonzero_and_all_centiles(self, nonzero_src, all_src, *pecentages):
        nonzero_centiles = nonzero_src.percentiles_only(*pecentages)
        all_centiles = all_src.percentiles_only(*pecentages)

        nonzero_count = nonzero_src.count()
        all_count = all_src.count()

        nonzero_portion = 100.0 * nonzero_count / all_count if all_count > 0 else 0
        all_portion = 100.0

        self.log(tabulate(
            [
                list(reversed(nonzero_centiles)) + [nonzero_count, nonzero_portion, 'nonzero'],
                list(reversed(all_centiles)) + [all_count, all_portion, 'all']
            ],
            headers=list(reversed(pecentages)) + ['count', '%', ''],
            floatfmt='.2f',
            tablefmt='grid'
        ))

    def _print_percentiles(self, source, *percentages):
        self.log(tabulate(
            [list(reversed(source.percentiles_only(*percentages)))],
            headers=list(reversed(percentages)),
            floatfmt='.2f',
            tablefmt='grid'
        ))

    def _print_percentiles_multi(self, name_source_list, *percentages):
        data = [list(reversed(list(source.percentiles_only(*percentages)))) + [name] for (name, source) in name_source_list]

        self.log(tabulate(
            data,
            headers=list(reversed(percentages)) + [''],
            floatfmt='.2f',
            tablefmt='grid'
        ))

    def _print_out_of(self, count, total_count):
        percentage = 100.0 * count / total_count if total_count > 0 else 0
        self.log('%.2f%% (%d out of %d)' % (percentage, count, total_count))
        self._print_line()

    def _print_line(self):
        self.log('-' * 20)


class RequestResult:
    def __init__(self, **info):
        self._info = info
        self.prev = None

    @property
    def status(self):
        return self._info.get('status')

    @property
    def message(self):
        return self._info.get('message')

    @property
    def category(self):
        return categorize_response(self.status, self.message)

    @property
    def time(self):
        return self._info['time']

    @property
    def duration(self):
        return self.time.duration

    @property
    def total_duration(self):
        request = self
        duration = 0.0
        while request is not None:
            duration += request.duration
            request = request.prev
        return duration

    def __str__(self):
        i = self._info
        return '{}: {} {} took {:.2f} ({} - {}) {}'.format(
            i['filename'], self.status or '---', i.get('rid'),
            self.time.duration, self.time.start_str, self.time.end_str,
            self.category
        )


def do_request(url, retry_count=0):
    def _do_request():
        with request_http(url) as r:
            try:
                r.perform()
                return RequestResult(
                    filename=r.filename, status=r.status, message=r.body, time=r.time, rid=r.header('X-Request-Id'))
            except pycurl.error as e:
                return RequestResult(filename=r.filename, message=e.args[1], time=r.time)

    prev_result = None
    result = None
    for _ in xrange(0, retry_count + 1):
        result = _do_request()
        result.prev = prev_result
        if ((result.status or 0) / 100) != 5:
            return result

    return result


class ReserveAwaitTime:
    def __init__(self):
        self.reserve = 0
        self.current_await = 0
        self.total_await = 0

    def add(self, delta, index):
        if delta < 0 and index == 0:
            return

        self.reserve += delta
        if self.reserve >= 0:
            self.current_await = 0
        else:
            self.current_await = abs(self.reserve)
            self.reserve = 0
        self.total_await += self.current_await


class Playlist:
    def __init__(self, stats, params, url, delete_cache=True):
        self.stats = stats
        self.params = params
        self.url = url + '&delete_cache=' + ('true' if delete_cache else 'false')

    def check_segments(self, proceed):
        time.sleep(random.uniform(0, 1) * 2)
        playlist_request = do_request(self.url)
        segments = self._split_segments(self._parse_segments(playlist_request), 0)
        reserve_await_time = ReserveAwaitTime()
        dimension = None
        for index, (segment_url, segment_duration) in enumerate(segments):
            if not proceed.is_set():
                break

            segment_request = do_request(segment_url, retry_count=1)
            if self.params.show_ok or segment_request.status != 200 or segment_request.duration >= self.params.long_request_threshold:
                self.log(segment_request)
                if segment_request.category == 'other':
                    self.log(segment_request.message)
                    self.log(segment_url)
                elif segment_request.category == 'Backend timeout':
                    self.log(segment_url)

            reserve_await_time.add(segment_duration - segment_request.duration, index)

            non_app_segment_time = None
            if segment_request.status == 200:
                try:
                    debug_info = json.loads(segment_request.message)
                    self.stats.rec_load_average(float(debug_info['loadAverage1min']), debug_info['source'])
                    non_app_segment_time = int(debug_info['duration']) / 1000.0
                    if dimension is None:
                        dimension = debug_info['dimension']
                except ValueError as e:
                    self.log(str(e))
                    pass

            self.stats.rec_segment(playlist_request, segment_request, index, segment_duration, reserve_await_time.current_await, non_app_segment_time)

        self.stats.rec_playlist_await(reserve_await_time.total_await, dimension)

    def log(self, value=''):
        self.params.log(value)

    @staticmethod
    def _split_segments(segments, split_index):
        return segments[split_index:] + segments[:split_index] if split_index != 0 else segments

    def _parse_segments(self, request):
        segment_data = re.findall('#EXTINF:(\d+),\s*\n\s*(\d+.ts)\s*', request.message, re.MULTILINE)
        return [(self._get_segment_url(uri), int(duration)) for (duration, uri) in segment_data]

    def _get_segment_url(self, segment_uri):
        return re.sub('playlist.m3u8', segment_uri, self.url)


class PlaylistProvider:
    def __init__(self, stats, params):
        self.stats = stats
        self.params = params
        self.url_providers = set(params.url_providers)
        self._lock = threading.Lock()
        self._playlists = list()

    def next_playlist(self):
        if self.params.repeat_ratio > 0 and len(self._playlists) > 0:
            p = random.random()
            if p < self.params.repeat_ratio:
                return random.choice(self._playlists)

        url = self._next_url()
        if url is not None:
            playlist = Playlist(self.stats, self.params, url)
            self._playlists.append(playlist)
            return playlist
        else:
            return random.choice(self._playlists)

    def _next_url(self):
        provider = self._next_provider()
        if provider is None:
            return None

        return provider()

    def _next_provider(self):
        if len(self.url_providers) == 0:
            return None

        with self._lock:
            return self.url_providers.pop() if len(self.url_providers) > 0 else None


class Runner:
    def __init__(self, params):
        self.params = params
        self.stats = Stats(params)

    def run(self):
        #random.shuffle(self.params.url_providers)
        playlist_provider = PlaylistProvider(self.stats, self.params)

        def _check_playlist(proceed):
            while proceed.is_set():
                playlist = playlist_provider.next_playlist()
                playlist.check_segments(proceed)

        run_multithreaded(
            _check_playlist,
            self.params.thread_count,
            duration=self.params.test_duration,
            final_target=self.stats.print_stats
        )
