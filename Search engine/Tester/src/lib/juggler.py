import json
import requests
import time
import datetime
import logging
from startrek_client.exceptions import Conflict
from search.mon.tester.src.lib.config import Config, VerticalConfig
from typing import Union

JUGGLER_BASE_URL = 'https://juggler.yandex-team.ru/'
JUGGLER_API_V1 = 'https://juggler-api.yandex-team.ru/api/'
JUGGLER_API_V2 = 'https://juggler-api.yandex-team.ru/v2/'
TICKENATOR_URL = 'https://tickenator.z.yandex-team.ru/api/tickenator.services.TickenatorService/'
logger = logging.getLogger(__name__)


class JugglerPushInitError(Exception):
    pass


class JugglerPush(object):
    GEOS = ['ALL', 'SAS', 'MAN', 'VLA', 'IVA', 'MYT']
    post_comment_max_attempts = 6

    def __init__(self, push_data, config: Config = None):
        if not isinstance(push_data, dict):
            logger.debug(f'wrong push {push_data}')
            raise ValueError('Invalid data')
        self.push_time = datetime.datetime.now()
        self.host = push_data.get('host_name')
        self.service = push_data.get('service_name')
        self.notification_hash = push_data.get('hash')
        self.check_conf = self.get_juggler_conf()
        self.abc = self.get_abc()
        self._push_data = push_data.copy()
        self.martydb_service = self.get_martydb_service()
        self._config = config.get(self.martydb_service['vertical'])
        self.max_children_mtime = 0
        for x in push_data['children']:
            if 'actual' in x and isinstance(x['actual'], dict):
                child_mtime = x['actual'].get('status_mtime', 0)
                if child_mtime is not None and child_mtime > self.max_children_mtime:
                    self.max_children_mtime = x['actual']['status_mtime']

        self.max_children_mtime = datetime.datetime.fromtimestamp(self.max_children_mtime).strftime('%Y-%m-%d %H:%M:%S')
        self.is_disaster = self.tags and 'is_disaster' in self.tags
        self._st_ticket = None
        self.history_comment_id = None

    def list_services_by_abc(self):
        prefix = "listServices"
        payload = {'abc__slug': self.abc}
        response = requests.post(f'{TICKENATOR_URL}{prefix}', json=payload)
        response_json = response.json()
        return response_json.get('objects')

    def get_martydb_service(self):
        tickenator_objects = self.list_services_by_abc()
        if not tickenator_objects:
            raise JugglerPushInitError(f'Service {self.abc} not found in MartyDB and will be ignored')
        elif not len(tickenator_objects) == 1:
            objects_names = [f'{x["vertical"]}:{x["service"]}' for x in tickenator_objects]
            logger.error(f'Found {len(tickenator_objects)} services: {", ".join(objects_names)}')
            raise JugglerPushInitError(f'Service {self.abc} is not unique in MartyDB and will be ignored')
        return tickenator_objects[0]

    @property
    def namespace(self):
        return self.check_conf.get('namespace')

    @property
    def description(self):
        return self._push_data.get('description')

    @property
    def tags(self):
        return self.check_conf.get('tags', [])

    @property
    def push_data(self):
        return self._push_data.copy()

    @property
    def status(self):
        return self._push_data.get('status')

    @property
    def flags(self):
        return self._push_data.get('flags')

    def get_juggler_conf(self):
        prefix = 'checks/get_checks_state'
        payload = {'filters': [{'host': self.host, 'service': self.service}], 'limit': 0}
        response = requests.post(f'{JUGGLER_API_V2}{prefix}', json=payload)
        items = response.json()['items']
        for item in items:
            if item['host'] == self.host and item['service'] == self.service:
                return item
        raise ValueError('Config not found')

    def get_abc(self):
        prefix = "namespaces/get_namespaces"
        payload = {"name": self.namespace}
        response = requests.post(f'{JUGGLER_API_V2}{prefix}', json=payload)
        result = response.json().get("items")[0].get("abc_service").replace('svc_', '')
        return result

    @property
    def geo(self):
        for tag in self.tags:
            tag = tag.lower()
            if tag.startswith('a_geo_'):
                result = tag[6:].upper()
                if result in self.GEOS:
                    return result
        return 'ALL'

    @property
    def dict(self):
        result = {
            'host_name': self.host,
            'service_name': self.service,
            'push_time': self.push_time,
            'namespace': self.namespace,
            'hash': self.notification_hash,
            'abc_slug': self.abc,
            'tickenator': self.martydb_service,
            'original_message': self.push_data,
            'disaster': self.is_disaster,
            'status': self.status,
            'flags': self.flags,
            'description': self.description,
            'max_children_mtime': self.max_children_mtime,
            'check_url': ''.join([
                f'{JUGGLER_BASE_URL}check_details/',
                f'?host={self.host}',
                f'&service={self.service}'
            ])
        }
        return result

    @property
    def ticket(self):
        return self._st_ticket

    def load_ticket(self, client, queue):
        start_time = (datetime.datetime.now() - self._config.ticket_age).strftime('%Y-%m-%d %H:%M:%S')
        st_query = ' '.join([
            f'Queue: {queue.upper()} AND Created: >= "{start_time}"',
            f'AND Tags: "juggler_host:{self.host}" AND Tags: "juggler_service:{self.service}"',
            f'AND Resolution: empty() "Sort By": Created DESC'
        ])
        try:
            logger.debug(st_query)
            issues = client.issues.find(st_query)
            issues = list(issues)
            logger.debug(f'Found issues: {", ".join(list(map(lambda x: x.key, issues)))}')
        except Exception as _e:
            logger.exception(f'Failed to load tiockets from ST: {_e}')
            return None
        if not issues:
            return None
        else:
            self._st_ticket = issues[0]
            return self._st_ticket

    @property
    def meta(self):
        result = []
        meta = self.check_conf.get('meta')
        if isinstance(meta, str):
            try:
                meta = json.loads(meta)
            except json.decoder.JSONDecodeError:
                logger.exception(f'Invalid json in meta: {meta}')
                return result
        if not isinstance(meta, dict):
            return result
        if 'urls' in meta:
            for item in meta['urls']:
                result.append(f'(({item["url"]} {item["title"]}))')
        return result

    def get_tickenator_payload(self, queue, append_original_message=False):
        begin_time = self.push_time.strftime("%d-%m-%y %H:%M:%S")
        juggler_time = int(time.mktime(self.push_time.timetuple()) * 1000)
        alert_url = ''.join([
            f'{JUGGLER_BASE_URL}check_details/',
            f'?host={self.host}',
            f'&service={self.service}',
            f'&last=25MINUTES&before={juggler_time + 60 * 5 * 1000}'
        ])
        if self.is_disaster:
            description = [
                f'Сработала disaster-проверка {alert_url} в {self.martydb_service["service"]}.',
            ]
            summary = f'Сработала disaster-проверка {self.host} {self.service}'
        else:
            description = [
                f'Сработала проверка {alert_url} в {self.martydb_service["service"]}.',
            ]
            summary = f'Сработала проверка {self.host} {self.service}'
        if 'is_html' in self.tags:
            description.append(f'Сообщение: <#{self.description}#>')
        else:
            description.append(f'Сообщение: %%{self.description}%%')

        description.extend(self.meta)
        if append_original_message:
            description.extend([
                '',
                '<{Оригинальное сообщение:',
                '%%',
                f'{self.push_data}',
                '%%',
                '}>'
            ])
        ticket_payload = {
            'summary': summary,
            'description': '\n'.join(description),
            'duty_actions': f'{begin_time} Дежурному смены отправлен алерт',
            'geo': 'ALL',
            'service': {
                'service': self.martydb_service['service'],
                'vertical': self.martydb_service['vertical'],
                'environment': 'prod'
            },
            'queue': queue,
            'time_from': juggler_time,
            'support_line': self._config.support_line,
            'author': 'robot-tickenator',
            'alert_source': 'juggler',
            'alert_name': f'{self.host}:{self.service}',
            'tags': [
                f'juggler_host:{self.host}',
                f'juggler_service:{self.service}',
                f'disaster:{self.is_disaster}'
            ],
            'sro_autosearch': True
        }
        return ticket_payload

    def create_ticket(self, queue, append_original_message=False):
        logger.info(''.join([
            f'Creating ticket in {queue} queue for notification {self.notification_hash} ',
            f'({self.host}:{self.service})'
        ]))
        payload = self.get_tickenator_payload(queue=queue, append_original_message=append_original_message)
        headers = {'content-type': 'application/json'}
        prefix = "createTicketSPI"
        response = requests.post(f'{TICKENATOR_URL}{prefix}', data=json.dumps(payload), headers=headers)
        result = response.json()
        logger.info(
            f'Created ticket {result["issueId"]} in {queue} queue for notification {self.notification_hash} '
            '({self.host}:{self.service})'
        )
        return result

    def create_comment(self, issue, append_original_message=False):
        alert_url = ''.join([
            f'{JUGGLER_BASE_URL}check_details/',
            f'?host={self.host}',
            f'&service={self.service}'
        ])
        if self.is_disaster:
            comment_text = [
                f'История состояний disaster-проверки (({alert_url} {self.host}:{self.service}))'
            ]
        else:
            comment_text = [
                f'История состояний проверки (({alert_url} {self.host}:{self.service}))'
            ]
        if append_original_message:
            comment_text.extend(['#|', '||Время последнего обновления статуса|Статус|Сообщение|JSON push-уведомления||'])
            row_pattern = '||{max_children_mtime}|(({check_url} {status}))|{description}|' \
                          '<{{Оригинальное сообщение:\n%%{original_message}%%}}>||'
        else:
            comment_text.extend(['#|', '||Время последнего обновления статуса|Статус|Сообщение||'])
            row_pattern = '||{max_children_mtime}|(({check_url} {status}))|{description}||'

        comment_text.append(row_pattern.format(**self.dict))
        comment_text.append('|#')
        issue.comments.create(text='\n'.join(comment_text), params={'notify': False})

    def increment_relapses(self, client, issue):
        attempts = 0
        while attempts < self.post_comment_max_attempts:
            attempts += 1
            try:
                issue = client.issues[issue.key]
                version = issue.version
                cur_val = issue.tickets
                if cur_val is None:
                    cur_val = 0
                cur_val += 1
                issue.update(tickets=cur_val, params={'version': version, 'notify': False})
                return True
            except Conflict as _e:
                logger.debug(f'Conflict on incrementing: {_e}', exc_info=True)
                continue
            except Exception as _e:
                logger.exception(f'Error on incrementing flaps counter due to {_e}')
        logger.debug(f'Failed to increment flaps counter on ticket {issue.key}')

    @property
    def history_comment(self):
        issue = self._st_ticket
        if issue.comments:
            for comment in issue.comments:
                if comment.createdBy.id == 'robot-tickenator' and comment.text.startswith('История состояний '):
                    self.history_comment_id = comment.id
                    return comment
        return None

    def append_comment(self, client, append_original_message=False):
        logger.debug(f'Aopending comment for {self._st_ticket}')
        issue = self._st_ticket
        if append_original_message:
            row_pattern = '||{max_children_mtime}|(({check_url} {status}))|{description}|' \
                          '<{{Оригинальное сообщение:\n%%{original_message}%%}}>||'
        else:
            row_pattern = '||{max_children_mtime}|(({check_url} {status}))|{description}||'
        comment_text = row_pattern.format(**self.dict)
        comment = self.history_comment
        if comment is not None:
            attempts = 0
            while attempts < self.post_comment_max_attempts:
                attempts += 1
                try:
                    cur_text = comment.text.split('|#')[0]
                    if self.max_children_mtime in cur_text:
                        logger.debug('This check is already posted')
                        return True
                    version = comment.version
                    new_text = f'{cur_text}\n{comment_text}\n|#'
                    comment.update(text=new_text, params={'version': version, 'notify': False})
                    if self.status == 'CRIT':
                        self.increment_relapses(client, issue)
                    logger.debug('Comment successfully posted')
                    return True
                except Conflict:
                    logger.debug('Conflict on posting comment, retrying')
                    self._st_ticket = client.issues[self._st_ticket.key]
                    comment = self._st_ticket.comments[self.history_comment_id]
                    continue
                except Exception as _e:
                    logger.exception(f'Failed to post comment due to {_e}')
                    continue
            logger.error('Failed to post due to conflicts')
        else:
            self.create_comment(issue, append_original_message)

    def process(self, client, queue, append_original_message=False):
        st_ticket = self.load_ticket(client=client, queue=queue)
        logger.debug(f'Processing check {self.host}:{self.service} ({self.notification_hash})')
        result = {}
        if st_ticket is None:
            logger.debug(f'Ticket not found: st_ticket is {st_ticket}')
            if self.status == 'CRIT':
                try:
                    result = {self.notification_hash: {
                        'ticket': self.create_ticket(queue, append_original_message=append_original_message),
                        'check': f'{self.host}:{self.service}'
                    }}
                except Exception as _e:
                    logger.exception(f'Failed to create ticket: {_e}')
            else:
                logger.debug(f'Check {self.host}:{self.service} is not in CRIT state and not tickets found to append history')
                return {}
        else:
            try:
                self.append_comment(client=client, append_original_message=append_original_message)
                result = {self.notification_hash: {
                    'ticket': st_ticket.key,
                    'check': f'{self.host}:{self.service}'
                }}
            except Exception as _e:
                logger.exception(f'Failed to append comment: {_e}')
        logger.debug(f'Check {self.host}:{self.service} ({self.notification_hash}) processed')
        return result


class JugglerPushes(object):
    def __init__(self, config: Config, pushes=None):
        self._pushes = []
        if isinstance(config, Config):
            self._config = config
        else:
            raise ValueError(f'config is {type(config)}, not Config')
        if isinstance(pushes, list):
            self.extend(pushes)
        elif isinstance(pushes, dict) and 'checks' in pushes:
            self.extend(pushes['checks'])
        elif pushes is not None:
            logger.critical(f'Invalid pushes: {type(pushes)}')

    def append(self, push):
        if not isinstance(push, JugglerPush):
            try:
                push = JugglerPush(push, self._config)
            except JugglerPushInitError as _e:
                logger.error(_e)
                return self
            except Exception as _e:
                logger.exception(_e)
                raise ValueError(f'Incorrect push type: {type(push)}')
        self._pushes.append(push)
        return self

    def extend(self, pushes, die_on_invalid_push=False):
        for push in pushes:
            try:
                self.append(push)
            except ValueError:
                if not die_on_invalid_push:
                    continue
                else:
                    raise
        return self

    def __iter__(self):
        return iter(self._pushes)

    def __getitem__(self, item):
        return self._pushes.__getitem__(item)

    def __len__(self):
        return len(self._pushes)

    def process_pushes(self, client, queue, append_original_message=False):
        result = {}

        for check in self._pushes:
            result.update(check.process(client=client, queue=queue, append_original_message=append_original_message))
        return result

