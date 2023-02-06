# -*- encoding: utf8 -*-

from .decorators import retry_method_on_exceptions
from abc import abstractmethod

import yt.wrapper as yt

import json
import logging
import requests
import time
import uuid

__all__ = ["ScraperBase", "_BASE_SCRAPER_URL"]

# https://git.qe-infra.yandex-team.ru/projects/SEARCH_INFRA/repos/serp-scraper/browse/api-parent/scrape-api/src/main/java/ru/yandex/qe/scraper/api/serp/batch/SerpSetScrapeStatus.java
_BATCH_STATUSES = {
    'progress': ('CREATING', 'PROGRESS', 'PAUSED'),
    'fail': ('FAIL', 'CANCELED'),
    'success': ('COMPLETE',),
}


_DEFAULT_HOSTS = {
    'ru': 'https://hamster.yandex.ru',
    'ua': 'https://hamster.yandex.ua',
    'by': 'https://hamster.yandex.by',
    'kz': 'https://hamster.yandex.kz',
    'com': 'https://hamster.yandex.com',
    'tr': 'https://hamster.yandex.com.tr'
}


_BASE_SCRAPER_URL = 'https://scraper.yandex-team.ru/api/scraper/batch'


class ScraperBase(object):
    def __init__(self, additional_cgi, task_description, args):
        if additional_cgi and not isinstance(additional_cgi, dict):
            raise ValueError("additional_cgi must be empty or dict")

        if task_description and not isinstance(task_description, dict):
            raise ValueError("task_description must be empty or dict")

        self.scraper_profile = args.scraper_profile
        self.scraper_ignore_profile_conflicts = args.scraper_ignore_profile_conflicts

        self.hosts = _DEFAULT_HOSTS.copy()
        if 'hosts' in args and args.hosts:
            self.hosts.update([host.split(':', 1) for host in args.hosts])

        self.additional_cgi = additional_cgi
        self.task_description = task_description

        self.queries_plan_table = args.queries_plan
        self.queries_batch_size = args.queries_batch_size

        self.status_request_delay = args.status_request_delay
        self.thumbs_per_page = args.thumbs_per_page
        self.scraper_user = args.scraper_user
        self.scraper_store_period = args.scraper_store_period
        self.serps_type = args.serps_type
        self.search_engine = args.search_engine

        if hasattr(args, 'use_scraper_over_yt'):
            self.use_scraper_over_yt = args.use_scraper_over_yt
            self.scraper_over_yt_pool = args.scraper_over_yt_pool

            if hasattr(args, 'scraper_over_yt_args'):
                self.scraper_over_yt_args = args.scraper_over_yt_args
            else:
                self.scraper_over_yt_args = {}
        else:
            self.use_scraper_over_yt = False

        self.session = requests.Session()
        self.session.headers = {
            'Content-type': 'application/json',
            'Accept': 'application/json',
            'Authorization': 'OAuth {0}'.format(args.oauth_token)
        }

        self.network_try_count = args.network_try_count
        self.network_retry_timeout = args.network_retry_timeout

    def download(self):
        for batch in self.on_get_queries_batches():
            if not batch["queries"]:
                break
            ticket = 'Unknown'
            try:
                ticket = self._send_queries_batch(batch)
                logging.info('Send queries batch with ticket {0}'.format(ticket))

                status = self._get_ticket_status(ticket)
                while status in _BATCH_STATUSES['progress']:
                    time.sleep(self.status_request_delay)
                    status = self._get_ticket_status(ticket)
                logging.info('Received confirmation of completion of SERPs for ticket {0}: {1}'.format(ticket, status))

                if status in _BATCH_STATUSES['fail']:
                    self.on_batch_failed(ticket, status)  # usually raises

                self.on_download_serps(ticket)
                logging.info('Downloaded SERPs in ticket {0}'.format(ticket))
            except Exception:
                logging.exception('Caught the exception while processing ticket {0}'.format(ticket))
        self.on_complete_download()

    def on_get_queries_batches(self):
        plan_size = yt.get(self.queries_plan_table + "/@row_count")
        start = 0
        while start < plan_size:
            result = dict(queries=[])
            path = "{0}[#{1}:#{2}]".format(self.queries_plan_table, start, start + self.queries_batch_size)
            for record in yt.read_table(path, format=yt.JsonFormat(attributes={'encode_utf8': False}), raw=False):
                query, tld, region = self.on_get_query_data(record)

                if "tld" not in result:
                    result["tld"] = tld
                if "region" not in result:
                    result["region"] = region
                if result["tld"] != tld or result["region"] != region:
                    break

                result["queries"].append(query)
                start += 1

            if result["queries"]:
                yield result

    @retry_method_on_exceptions
    def _send_queries_batch(self, batch):
        request_data = self.on_create_request_data(batch)
        request = self.session.post(_BASE_SCRAPER_URL, data=json.dumps(request_data), verify=False)
        if request.status_code == 400:
            logging.error('Bad request to scraper: {0}'.format(request.text))
        request.raise_for_status()

        return request.json()['ticket']

    def on_create_request_data(self, batch):
        tld_to_region_id = {
            'ru':     225,
            'ua':     187,
            'by':     149,
            'kz':     159,
            'com':    983,
            'tr':      84
        }

        per_set_params = {
            'results-per-page': self.thumbs_per_page
        }

        tld = batch["tld"]
        assert tld is None or tld in tld_to_region_id, 'Wrong TLD value: {0}!'.format(tld)
        region = batch["region"]

        if tld or region:
            per_set_params['region-id'] = tld_to_region_id[tld] if region is None else region

        if self.additional_cgi:
            per_set_params['additional-cgi'] = self.additional_cgi

        queries_info = []
        for query in batch["queries"]:
            serp_id = str(uuid.uuid4())
            query_info = {
                'per-query-parameters': {
                    'query-text': query
                },
                'serp-request-id': serp_id
            }
            queries_info.append(query_info)

        request_field = {
            'host': self.hosts[tld or 'ru'],
            'per-set-parameters': per_set_params,
            'store-results-period': self.scraper_store_period,
            'search-engine': self.search_engine,
            'parse-serps': True,
            'description': self.task_description,
            'profile': self.scraper_profile,
            'ignore-profile-conflicts': self.scraper_ignore_profile_conflicts
        }

        if self.use_scraper_over_yt:
            overYtParams = {
                'scraper-over-yt-pool': self.scraper_over_yt_pool,
                'args': self.scraper_over_yt_args
            }
            request_field['over-yt-parameters'] = overYtParams

        request_data = {
            'queries': queries_info,
            'request': request_field
        }

        return request_data

    @retry_method_on_exceptions
    def _get_ticket_status(self, ticket):
        request_url = '{0}/{1}/status'.format(_BASE_SCRAPER_URL, ticket)

        request = self.session.get(request_url, verify=False)
        if request.status_code != 200:
            raise Exception('Received {0} status code for ticket {1} completion request'.format(request.status_code, ticket))

        status = request.json()['status']
        logging.info('Ticket status: {0}. Completed serps: {1}'.format(status, request.json()['completed-serps']))
        return status

    def on_batch_failed(self, ticket, status):
        """ Virtual
        """
        raise Exception('Ticket {0} finished unsuccessfully: {1}'.format(ticket, status))

    @abstractmethod
    def on_download_serps(self, ticket):
        pass

    def on_get_query_data(self, row):
        """ Virtual
        """
        try:
            region = int(row['region'])
        except (KeyError, TypeError, ValueError):
            region = None
        return row["query"], row.get("tld"), region
