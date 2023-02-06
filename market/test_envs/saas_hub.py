# coding: utf-8

import collections
import ctypes
import json
import logging
import os
import re
import socket
import threading
import time

import psutil
import requests

from subprocess import Popen, PIPE

from market.idx.yatf.common import get_binary_path
from market.pylibrary.putil.protector import retry
from market.idx.yatf.matchers.env_matchers import HasExitCode
from market.idx.yatf.test_envs.base_env import BaseEnv
from yatest.common.network import PortManager

from market.idx.yatf.resources.category_restrictions_pb import CategoryRestrictions
from market.proto.content.mbo.Restrictions_pb2 import RestrictionsData

logger = logging.getLogger(__name__)


def int32_to_uint32(i):
    return ctypes.c_uint32(i).value


def build_price(hi_word, lo_word, precision=7):
    return float(int32_to_uint32(hi_word) * 2**32 + int32_to_uint32(lo_word)) / 10**precision


def build_uint64(hi_word, lo_word):
    return int32_to_uint32(hi_word) * 2**32 + int32_to_uint32(lo_word)


def build_offer_disabled(value_bits, mask_bits):
    """
    Построение значения битовой маски
    :param value_bits: Биты признака скрытия (младшие 16 бит). Значение 1 в бите => оффер скрыт по соответствующему
    номеру бита источнику из OfferMeta::DataSource.
    :param mask_bits: Биты значимости младших 16ти бит (значение 0 => соответсвующий бит из младших 16 is unknown).
    :return: Значение offer_disabled в saas-hub и RTY
    """
    result = 0
    for b in value_bits:
        result |= 1 << b
    for b in mask_bits:
        result |= 1 << (b + 16)
    return result


def parse_output_doc(line):
    parsed_msg = json.loads(line)
    raw_doc = parsed_msg['docs'][0]
    # required fields
    out_doc = {
        'action': parsed_msg['action'],
        'prefix': parsed_msg['prefix'],
        'url': raw_doc['url'],
    }

    # optional fields
    if 'options' in raw_doc:
        if 'modification_timestamp' in raw_doc['options']:
            out_doc['modification_timestamp'] = raw_doc['options']['modification_timestamp']
        if 'version' in raw_doc['options']:
            out_doc['version'] = raw_doc['options']['version']
    if 'price_high' in raw_doc:
        out_doc['price'] = build_price(
            raw_doc['price_high'][0]['value'],
            raw_doc['price_low'][0]['value']
        )
    if 'old_price_high' in raw_doc:
        out_doc['old_price'] = build_price(
            raw_doc['old_price_high'][0]['value'],
            raw_doc['old_price_low'][0]['value']
        )
    if 'partner_price_high' in raw_doc:
        out_doc['partner_price'] = build_price(
            raw_doc['partner_price_high'][0]['value'],
            raw_doc['partner_price_low'][0]['value']
        )
    if 'update_time_high' in raw_doc:
        out_doc['update_time'] = build_uint64(
            raw_doc['update_time_high'][0]['value'],
            raw_doc['update_time_low'][0]['value']
        )
    if 'changed_states' in raw_doc:
        out_doc['changed_states'] = raw_doc['changed_states'][0]['value']
    if 'download_ts' in raw_doc:
        out_doc['download_ts'] = raw_doc['download_ts'][0]['value']
    if 'shard' in parsed_msg:
        out_doc['shard'] = parsed_msg['shard']

    for i in range(1, 10):
        amore_field_name = 'amore_data_' + str(i)
        if amore_field_name in raw_doc:
            out_doc[amore_field_name] = int32_to_uint32(raw_doc[amore_field_name][0]['value'])
        else:
            break

    def fill_simple_field(name, conv=None):
        if name in raw_doc:
            out_doc[name] = raw_doc[name][0]['value']
            if conv:
                out_doc[name] = conv(out_doc[name])

    fill_simple_field('flags', conv=int32_to_uint32)
    fill_simple_field('flags_ts')
    fill_simple_field('bid')
    fill_simple_field('bid_ts')
    fill_simple_field('fee')
    fill_simple_field('fee_ts')
    fill_simple_field('bid_and_flags')
    fill_simple_field('bid_and_flags_ts')
    fill_simple_field('offer_disabled')
    fill_simple_field('offer_disabled_ts')
    fill_simple_field('refill_ts')
    fill_simple_field('refill_changed_ts')
    fill_simple_field('order_method')
    fill_simple_field('order_method_ts')
    fill_simple_field('vendor_charge')
    fill_simple_field('market_commission')
    fill_simple_field('market_multiplier')
    fill_simple_field('dont_pull_up_bids')
    fill_simple_field('dont_pull_up_bids_ts')
    fill_simple_field('amore_data_ts')
    for k, v in raw_doc.iteritems():
        bucket_prefix = 'delivery_bucket_'
        if k.startswith(bucket_prefix) and v['type'] == '#Q':
            bucket_id = int(k[len(bucket_prefix):])
            if 'delivery_buckets' not in out_doc:
                out_doc['delivery_buckets'] = {}
            out_doc['delivery_buckets'][bucket_id] = {}
            names = v['value']['factor_names']
            indexes = {}
            for i in xrange(len(names)):
                indexes[names[i]] = i
            for reg, factors in v['value']['factors'].iteritems():
                def get_factor(name):
                    if name not in indexes:
                        return 0
                    index = indexes[name]
                    return factors[index] if len(factors) > index else 0
                region = int(reg.split('/')[0])
                if region not in out_doc['delivery_buckets'][bucket_id]:
                    out_doc['delivery_buckets'][bucket_id][region] = []
                price = build_uint64(get_factor('price_high'), get_factor('price_low'))
                if price == 0xFFFFFFFFFFFFFFFF:
                    out_doc['delivery_buckets'][bucket_id][region].append('forbidden')
                elif price == 0xFFFFFFFFFFFFFFFE:
                    out_doc['delivery_buckets'][bucket_id][region].append('unspecific')
                else:
                    out_doc['delivery_buckets'][bucket_id][region].append({
                        'day_from': get_factor('day_from'),
                        'day_to': get_factor('day_to'),
                        'order_before': get_factor('order_before'),
                        'price': price,
                        'shop_price': build_uint64(get_factor('shop_price_high'), get_factor('shop_price_low')),
                    })
    return out_doc


class OutStreamTracker(object):
    def __init__(self):
        self._out_file = None
        self._out_buffer = collections.deque()
        self._lock = threading.Lock()

    def open(self, command=None, process=None, out_file=None, err_file=None):
        raise NotImplementedError

    def close(self):
        self._out_file.close()

    def __call__(self, *args, **kwargs):
        self._read()

    def next_line(self):
        self._read()
        if not self._out_buffer:
            return None
        return self._out_buffer.popleft()

    def skip_all(self):
        self._read()
        self._out_buffer.clear()

    def _read(self):
        for line in self._out_file.readlines():
            line = line.strip()
            if line:
                self._out_buffer.append(line)


class StdOutTracker(OutStreamTracker):
    def open(self, command=None, process=None, out_file=None, err_file=None):
        logger.debug('track stdout: {}'.format(out_file.name))
        self._out_file = open(out_file.name, 'rt')


class LogTracker(OutStreamTracker):
    def __init__(self):
        super(LogTracker, self).__init__()
        self._log_file_path = None

    def open(self, command=None, process=None, out_file=None, err_file=None):
        self._log_file_path = os.path.join(psutil.Process(process.pid).cwd(), out_file)
        logger.debug('track log file: {}'.format(self._log_file_path))
        try:
            self._open()
        except IOError:
            # the file may not have been created yet
            pass

    def close(self):
        if self._out_file is not None:
            self._out_file.close()

    def _open(self):
        self._out_file = open(self._log_file_path, 'rt')

    def _read(self):
        if self._out_file is None:
            self._open()
        super(LogTracker, self)._read()


class SaasHubTestEnv(BaseEnv):
    _MATCHERS = [
        HasExitCode(0)
    ]

    def __init__(self, port_manager=None, **resources):
        assert 'saas_hub_cfg' in resources, 'saas_hub_cfg must be defined'
        super(SaasHubTestEnv, self).__init__(**resources)

        self._port_manager = port_manager
        self._its_config_path = None
        self._stdout_tracker = StdOutTracker()
        self._log_tracker = LogTracker()
        self._server_log_file = "server.log"

        resources_stubs = {
            'category_restrictions': CategoryRestrictions(RestrictionsData())
        }

        for name, val in resources_stubs.iteritems():
            if name not in self.resources:
                self.resources[name] = val

    @property
    def description(self):
        return 'saas_hub'

    @property
    def port_manager(self):
        return self._port_manager

    @staticmethod
    def binary_path():
        relative_path = os.path.join('market', 'idx', 'quick', 'saashub', 'bin', 'market_saas_hub')
        return get_binary_path(relative_path)

    def __enter__(self, path=None):
        BaseEnv.__enter__(self)
        if path is None:
            path = SaasHubTestEnv.binary_path()

        saas_hub_config = os.path.join(
            self.input_dir,
            self.resources['saas_hub_cfg'].filename
        )

        self._its_config_path = None
        if 'its_config' in self.resources:
            its_config = self.resources['its_config']
            its_config.init(self)
            self._its_config_path = its_config.path

        if self._port_manager is None:
            with PortManager() as pm:
                self._controller_port = pm.get_port()
                self._neh_port = pm.get_port()
                self._http_port = pm.get_port()
        else:
            self._controller_port = self._port_manager.get_port()
            self._neh_port = self._port_manager.get_port()
            self._http_port = self._port_manager.get_port()

        env = {
            'CONTROLLER_PORT': str(self._controller_port),
            'NEH_PORT': str(self._neh_port),
            'HTTP_PORT': str(self._http_port)
        }

        cmd = [path, saas_hub_config]
        for k, v in env.iteritems():
            cmd += ['-V', '{k}={v}'.format(k=k, v=v)]
        if self._its_config_path is not None:
            cmd += ['--its', self._its_config_path]
        self.exec_result = self.try_execute_under_gdb(
            cmd,
            cwd=self.output_dir,
            wait=False,
            check_exit_code=False,
            process_progress_listener=self._stdout_tracker
        )

        self._log_tracker.open(process=self.exec_result.process, out_file=self._server_log_file)

        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        for i in range(60):
            time.sleep(1)
            if not self.exec_result.running:
                logging.error('saas_hub failed, exit_code=%s, check_num=%s' % (self.exec_result.exit_code, i))
            assert self.exec_result.running
            if not sock.connect_ex(('localhost', self._controller_port)):
                return self
        raise Exception('saas_hub not started')

    def __exit__(self, *args):
        BaseEnv.__exit__(self, args)
        if self.exec_result is not None and self.exec_result.running:
            self.exec_result.kill()
        self.exec_result = None
        self._log_tracker.close()

    def _send_message(self, msg_obj, mode, pq_opts=None):
        sender_path = os.path.join('market', 'idx', 'quick', 'saashub', 'tools', 'sender', 'sender')
        cmd_list = [
            get_binary_path(sender_path),
            '--304-is-error',
            '--mode={}'.format(mode),
            '--verbose',
        ]
        if pq_opts:
            cmd_list += [
                '--pq-host={}'.format(pq_opts['host']),
                '--pq-port={}'.format(pq_opts['port']),
                '--pq-topic={}'.format(pq_opts['topic']),
                '--pq-source-id={}'.format(['source-id']),
            ]
        else:
            cmd_list.append('--address={}'.format('{}:{}'.format(self.host, self.neh_port)))

        proc = Popen(cmd_list, stdin=PIPE, stderr=PIPE, stdout=PIPE)
        stdout, stderr = proc.communicate(input=json.dumps(msg_obj))
        code, status = stderr.split(':', 1)
        logger.info('msg_obj: %s; code: %s; status: %s', msg_obj, code, status)
        return int(code), status

    def _send_batch(self, path, mode, pq_opts=False, cmd_ext=None):
        sender_path = os.path.join('market', 'idx', 'quick', 'saashub', 'tools', 'sender', 'sender')
        cmd_list = [
            get_binary_path(sender_path),
            '--mode={}'.format(mode),
        ]

        if pq_opts:
            cmd_list += [
                '--pq-host={}'.format(pq_opts['host']),
                '--pq-port={}'.format(pq_opts['port']),
                '--pq-topic={}'.format(pq_opts['topic']),
                '--pq-source-id={}'.format(['source-id']),
            ]
        else:
            cmd_list.append('--address={}'.format('{}:{}'.format(self.host, self.neh_port)))

        cmd_list += [
            '--format={}'.format('pbsn'),
            path,
        ]

        if cmd_ext:
            cmd_list += cmd_ext

        proc = Popen(cmd_list, stderr=PIPE, stdout=PIPE)
        _, stderr = proc.communicate()
        output = (line.split(':', 1) for line in stderr.split('\n') if ':' in line)
        return proc.returncode, [(int(code), status) for code, status in output]

    @property
    def its_config_path(self):
        return self._its_config_path

    @property
    def output_docs(self):
        """Stops current process to get its output
           (yatest hasn't got other option)."""
        self.exec_result.kill()
        for line in self.exec_result.std_out.splitlines():
            try:
                yield parse_output_doc(line)
            except Exception as e:
                logger.warning('cannot parse line: %s (%s: %s)', line, type(e).__name__, e)

    def wait_output_doc_gen(self, doc_number, sleep_time=.1):
        logger.info('waiting while output {} documents'.format(doc_number))
        doc_counter = 0
        while doc_counter != doc_number:
            line = self._stdout_tracker.next_line()
            if not line:
                if not self.exec_result.running:
                    raise RuntimeError('saashub already finished')
                time.sleep(sleep_time)
                continue
            try:
                doc = parse_output_doc(line)
                doc_counter += 1
                logger.debug('document received')
                yield doc
            except Exception as e:
                logger.warning('cannot parse line: %s (%s: %s)', line, type(e).__name__, e)

    def wait_output_doc(self, doc_number, sleep_time=.1):
        try:
            for _ in self.wait_output_doc_gen(doc_number, sleep_time=sleep_time):
                pass
        except StopIteration:
            pass

    def wait_log_line(self, regex, sleep_time=.1, skip_before=False, skip_n=0):
        logger.info('waiting log line, regex: {}'.format(regex))
        if skip_before:
            self._log_tracker.skip_all()
        r = re.compile(regex)
        found_line_count = 0
        while True:
            line = self._log_tracker.next_line()
            if not line:
                if not self.exec_result.running:
                    raise RuntimeError('saashub already finished')
                time.sleep(sleep_time)
                continue
            if r.search(line) is not None:
                found_line_count += 1
                logger.debug('line found: {}'.format(line))
                if found_line_count <= skip_n:
                    logger.debug('skip {} found line, line count {}'.format(skip_n, found_line_count))
                else:
                    break

    def send(self, qoffer, mode, pq_opts=None):
        return self._send_message(qoffer, mode, pq_opts)

    def send_qoffer(self, qoffer, pq_opts=None):
        return self._send_message(qoffer, 'qoffer', pq_opts)

    def send_soffer(self, soffer, pq_opts=None):
        return self._send_message(soffer, 'soffer', pq_opts)

    def send_api_offer(self, api_offer, pq_opts=None):
        return self._send_message(api_offer, 'api_offer', pq_opts)

    def send_feed_delivery_options(self, options, pq_opts=None):
        return self._send_message(options, 'feed_delivery_options', pq_opts)

    def delete_offer(self, offer, pq_opts=None):
        return self._send_message(offer, 'delete', pq_opts)

    def send_api_offer_batch(self, path, pq_opts=None):
        return self._send_batch(path, 'api_offer', pq_opts)

    def send_api_qbids_batch(self, path, pq_opts=None, timestamp=None):
        cmd_ext = None
        if timestamp:
            cmd_ext = ['--qbid-timestamp', str(timestamp)]
        return self._send_batch(path, 'api_qbids', pq_opts, cmd_ext=cmd_ext)

    def send_data_camp_offer(self, data_camp_offer, pq_opts=None):
        return self._send_message(data_camp_offer, 'data_camp_offer', pq_opts)

    def send_data_camp_offers_batch(self, data_camp_offer, pq_opts=None):
        return self._send_message(data_camp_offer, 'data_camp_offers_batch', pq_opts)

    def send_data_camp_offers_batch_native(self, data_camp_offer, pq_opts):
        return self._send_message(data_camp_offer, 'data_camp_offers_batch_native', pq_opts)

    def send_data_camp_message(self, data_camp_message, pq_opts):
        return self._send_message(data_camp_message, 'data_camp_message', pq_opts)

    def send_delivery_bucket(self, bucket, pq_opts=None):
        return self._send_message(bucket, 'delivery_bucket', pq_opts)

    def send_rty_server_message(self, message, pq_opts):
        return self._send_message(message, 'rty_server_message', pq_opts)

    def doc_state(self, feed_id, offer_id):
        resp = requests.get(
            'http://{host}:{port}/doc_state/{feed_id}/{offer_id}'.format(
                host=self.host,
                port=self._http_port,
                feed_id=feed_id,
                offer_id=offer_id,
            )
        )
        logger.info('response: {}'.format(resp.content))
        return resp

    def delivery_bucket(self, bucket_ids, region_ids=None):
        url = 'http://{host}:{port}/delivery_bucket?format=json'.format(host=self.host, port=self._http_port)
        if not isinstance(bucket_ids, list):
            bucket_ids = [bucket_ids]
        if region_ids is None:
            region_ids = []
        if not isinstance(region_ids, list):
            region_ids = [region_ids]
        buckets = '&'.join('id={}'.format(bucket_id) for bucket_id in bucket_ids)
        regions = '&'.join('region={}'.format(region_id) for region_id in region_ids)
        if buckets:
            url += '&' + buckets
        if regions:
            url += '&' + regions
        resp = requests.get(url)
        logger.info('response: {}'.format(resp.content))
        return resp

    def delivery_bucket_groups_get(self,
                                   courier_buckets, pickup_buckets, post_buckets,
                                   courier_regions=None, pickup_regions=None, post_regions=None):
        def _format_group(bucket_param, region_param, buckets, regions):
            if len(buckets) == 0:
                return ''
            bucket_ids = '&'.join('{}={}'.format(bucket_param, bucket_id) for bucket_id in buckets)
            region_ids = '&'.join('{}={}'.format(region_param, region_id) for region_id in regions)
            if len(regions) > 0:
                region_ids = '&' + region_ids
            return '&' + bucket_ids + region_ids

        courier_regions = courier_regions or []
        pickup_regions = pickup_regions or []
        post_regions = post_regions or []
        url = 'http://{host}:{port}/delivery_bucket_groups?format=json'.format(host=self.host, port=self._http_port)
        url += _format_group('courier_id', 'courier_rid', courier_buckets, courier_regions)
        url += _format_group('pickup_id', 'pickup_rid', pickup_buckets, pickup_regions)
        url += _format_group('post_id', 'post_rid', post_buckets, post_regions)
        resp = requests.get(url)
        logger.info('response: {}'.format(resp.content))
        return resp

    def delivery_bucket_groups_post(self,
                                    courier_buckets, pickup_buckets, post_buckets,
                                    courier_regions=None, pickup_regions=None, post_regions=None):
        def _format_buckets(buckets):
            return [str(bucket) for bucket in buckets]

        url = 'http://{host}:{port}/delivery_bucket_groups?format=json'.format(host=self.host, port=self._http_port)
        resp = requests.post(url, json={
            'courier_id': _format_buckets(courier_buckets),
            'courier_rid': courier_regions or [],
            'pickup_id': _format_buckets(pickup_buckets),
            'pickup_rid': pickup_regions or [],
            'post_id': _format_buckets(post_buckets),
            'post_rid': post_regions or [],
        })
        logger.info('response: {}'.format(resp.content))
        return resp

    def tass(self, find_signals=None):
        resp = requests.get('http://{host}:{port}/tass'.format(host=self.host, port=self.controller_port))
        resp.raise_for_status()
        if find_signals is not None:
            result = {}
            for signal_name in find_signals:
                regex = r'\["{}",([\[\],\s\d]+?)\]'.format(signal_name)
                match = re.search(regex, resp.content)
                if match:
                    logger.debug('signal found => {}: {}'.format(signal_name, match.group(1)))
                    result[signal_name] = match.group(1)
            return result
        return resp.content

    def get_status(self):
        resp_json = self.send_http('?command=get_status', self._controller_port)
        return resp_json['status']

    def send_http(self, uri, port=None):
        port = port or self._http_port
        resp = requests.get(
            'http://{host}:{port}/{uri}'.format(
                host=self.host,
                port=port,
                uri=uri
            )
        )

        logger.info('common_proxy status: {}'.format(resp.content))
        resp.raise_for_status()
        resp_json = json.loads(resp.content)
        return resp_json

    def wait_activation(self):
        @retry(10, timeout=1)
        def get_active():
            status = self.get_status()
            if status == 'Active':
                return
            else:
                raise Exception
        get_active()

    @property
    def host(self):
        return 'localhost'

    @property
    def controller_port(self):
        return self._controller_port

    @property
    def neh_port(self):
        return self._neh_port

    @property
    def http_port(self):
        return self._http_port
