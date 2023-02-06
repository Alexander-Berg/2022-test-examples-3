# coding: utf-8

import logging
import time

from search.priemka.yappy.proto.tasklets.run_thirium_tests_tasklet import RunThiriumTestsBase
from search.priemka.yappy.proto.tasklets.run_thirium_tests_pb2 import ThiriumLaunchStatus

from ci.tasklet.common.proto import service_pb2 as ci
from sandbox.common.errors import TaskError
from tasklet.services.yav.proto.yav_pb2 import YavSecretSpec
from search.martylib.protobuf_utils.patch import patch_enums

from search.martylib.http import AbstractHttpClient

patch_enums()


class RunThiriumTestsImpl(RunThiriumTestsBase):
    # Default values
    THIRIUM_URL = 'https://backend-prod.thirium.yandex-team.ru'
    LAUNCH_URL = 'api/v2/launch'
    INFO_URL = 'https://th.yandex-team.ru/launches'
    THIRIUM_TOKEN_KEY = 'thirium.token'

    THIRIUM_PROJECT = None

    WAIT_SLEEP_BEFORE_CHECK = 0
    WAIT_SLEEP_STEP = 10
    WAIT_MAX_RETRIES = 5
    WAIT_RETRY_STEP_FACTOR = 1.5

    FINISHED_LAUNCH_STATUSES = (ThiriumLaunchStatus.FINISHED, ThiriumLaunchStatus.FAILED)
    SUCCESS_LAUNCH_STATUSES = (ThiriumLaunchStatus.FINISHED, )

    LAUNCH_STATUSES_CI = {
        ThiriumLaunchStatus.UNKNOWN: ci.TaskletProgress.Status.RUNNING,
        ThiriumLaunchStatus.NEW: ci.TaskletProgress.Status.RUNNING,
        ThiriumLaunchStatus.RUNNING: ci.TaskletProgress.Status.RUNNING,
        ThiriumLaunchStatus.FINISHED: ci.TaskletProgress.Status.SUCCESSFUL,
        ThiriumLaunchStatus.FAILED: ci.TaskletProgress.Status.FAILED,
    }

    # Internal variables
    _thirium_token = None
    _thirium = None
    _thirium_progress = None

    @property
    def thirium_progress(self):
        if not self._thirium_progress:
            self._thirium_progress = ci.TaskletProgress()
            self._thirium_progress.job_instance_id.CopyFrom(self.input.context.job_instance_id)
            self._thirium_progress.id = 'ThiriumTestsLaunch'
        if not self._thirium_progress.url and self.output.launch.id:
            self._thirium_progress.url = '{}/{}'.format(self.INFO_URL, self.output.launch.id)
        return self._thirium_progress

    @property
    def thirium(self):
        if not self._thirium:
            self._thirium = AbstractHttpClient(
                auth_required=True,
                base_url=self.THIRIUM_URL,
                oauth_token=self.thirium_token,
                content_type='application/json',
                load_json=True,
            )
        return self._thirium

    @property
    def thirium_token(self) -> str:
        if not self._thirium_token:
            self._thirium_token = self.get_yav_secret(self.THIRIUM_TOKEN_SECRET, self.THIRIUM_TOKEN_KEY)
        return self._thirium_token

    @property
    def test_launch_params(self):
        return {
            "capabilities.browserName": {
                "type": "CONSTANT",
                "value": "chrome"
            },
            "capabilities.browserVersion": {
                "type": "CONSTANT",
                "value": "90.0"
            }
        }

    @property
    def run_tests_request(self):
        if not self.THIRIUM_PROJECT:
            raise TaskError("Thirium project must be specified")

        request = {
            "projects": [self.THIRIUM_PROJECT],
            "params": self.test_launch_params,
            "snapshotStatuses": ["DIRTY"],  # THIRIUM-955: for now is necessary, but is to be removed one day
        }
        logging.debug('Construnted request: %s', request)
        return request

    def get_yav_secret(self, secret_id, key):
        if not secret_id:
            raise TaskError("Secret ID is not configured for key: '{}'".format(key))
        try:
            return self.ctx.yav.get_secret(YavSecretSpec(uuid=secret_id, key=key)).secret
        except Exception as err:
            raise TaskError("Failed to get key from YaV ('{}'): '{}' ({})".format(secret_id, key, err.details()))

    def configure(self):
        logging.info('Applying job configuration')
        config = self.input.config
        self.THIRIUM_URL = config.thirium.url or self.THIRIUM_URL
        self.THIRIUM_TOKEN_KEY = config.thirium.token_key or self.THIRIUM_TOKEN_KEY
        self.THIRIUM_TOKEN_SECRET = config.thirium.secret or config.secret or self.input.context.secret_uid
        self.THIRIUM_PROJECT = config.thirium.project
        self.WAIT_SLEEP_STEP = config.wait.sleep_step or self.WAIT_SLEEP_STEP
        self.WAIT_SLEEP_BEFORE_CHECK = (
            config.wait.sleep_before_check or
            self.WAIT_SLEEP_BEFORE_CHECK or
            self.WAIT_SLEEP_STEP
        )
        self.WAIT_MAX_RETRIES = config.wait.max_retries or self.WAIT_MAX_RETRIES
        self.WAIT_RETRY_STEP_FACTOR = config.wait.retry_step_factor or self.WAIT_RETRY_STEP_FACTOR

    def run_tests(self):
        logging.info("Create Thirium test launch for project: '%s'", self.THIRIUM_PROJECT)
        result = self.thirium.session.post(self.LAUNCH_URL, json=self.run_tests_request)
        logging.info('Thirium response: %s', result)
        launch_id = result.get('id')
        if not launch_id:
            raise TaskError('Failed to run tests')
        return launch_id

    def get_launch_info(self, launch_id):
        logging.info('Getting launch info')
        response = self.thirium.session.get('{}/{}'.format(self.LAUNCH_URL, launch_id))
        self.update_progress(response)
        logging.debug('Launch info: %s', response)
        return response

    def get_launch_status(self, launch_id):
        # (int) -> ThiriumLaunchStatus
        logging.info('Getting test launch state')
        launch_info = self.get_launch_info(launch_id)
        status = self.get_launch_status_from_info(launch_info)
        logging.info('Current status: %s', ThiriumLaunchStatus[status])
        return status

    def get_launch_status_from_info(self, launch_info):
        # (dict) -> ThiriumLaunchStatus
        return ThiriumLaunchStatus[launch_info.get("status", "UNKNOWN")]

    def sleep_intervals(self):
        yield self.WAIT_SLEEP_BEFORE_CHECK
        sleep_sec = self.WAIT_SLEEP_STEP
        yield sleep_sec
        for _ in range(self.WAIT_MAX_RETRIES):
            sleep_sec *= self.WAIT_RETRY_STEP_FACTOR
            yield sleep_sec

    def wait_for_completion(self, launch_id):
        logging.info('Waiting test launch to finish')
        total_sec = 0
        for sleep_sec in self.sleep_intervals():
            total_sec += sleep_sec
            logging.debug('Sleep %s sec', sleep_sec)
            time.sleep(sleep_sec)
            status = self.get_launch_status(launch_id)
            if status in self.FINISHED_LAUNCH_STATUSES:
                return status
        raise TaskError("Tests didn't finish in {} sec. Stop waiting.".format(total_sec))

    def update_output(self):
        logging.info('Setting output data')
        launch_info = self.get_launch_info(self.output.launch.id)
        self.output.launch.details_url = self.thirium_progress.url
        self.output.launch.status = self.get_launch_status_from_info(launch_info)

    def update_progress(self, launch_info=None):
        if not self.output.launch.id:
            return
        if not launch_info:
            launch_info = self.get_launch_info(self.output.launch.id)
        status = self.get_launch_status_from_info(launch_info)
        percent = launch_info.get('finishedPercent', 0)
        self.thirium_progress.status = self.LAUNCH_STATUSES_CI.get(status, ci.TaskletProgress.Status.RUNNING)
        self.thirium_progress.progress = percent * 0.01
        self.thirium_progress.text = "Running tests ({}% complete)".format(percent)
        self.ctx.ci.UpdateProgress(self.thirium_progress)

    def run(self):
        logging.info('Running CI job: RunThiriumTests')
        self.configure()
        self.output.launch.id = self.run_tests()
        self.update_progress()
        status = self.wait_for_completion(self.output.launch.id)
        self.update_output()
        if status not in self.SUCCESS_LAUNCH_STATUSES:
            raise TaskError('Tests failed')
        self.output.state.success = True
