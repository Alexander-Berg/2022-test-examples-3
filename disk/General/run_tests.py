import logging
import requests
import time
import xml.etree.ElementTree as et

from disk.tasklets.teamcity.proto import run_tests_tasklet
from tasklet.services.yav.proto import yav_pb2
from ci.tasklet.common.proto import service_pb2 as ci

TEAMCITY_API_URL = "https://teamcity.yandex-team.ru"

logger = logging.getLogger(__name__)


class RunTestsImpl(run_tests_tasklet.RunTestsBase):

    @staticmethod
    def _get_teamcity_build_xml(build_type, properties):
        build = et.Element("build")
        et.SubElement(build, "buildType", id=build_type)
        properties_xml = et.SubElement(build, "properties")
        for name, value in properties.items():
            et.SubElement(properties_xml, "property", name=name, value=value)
        return et.tostring(build, encoding="utf-8")

    def run(self):
        progress_msg = ci.TaskletProgress()
        progress_msg.job_instance_id.CopyFrom(self.input.context.job_instance_id)
        progress_msg.progress = 0.0
        progress_msg.module = "TEAMCITY"
        self.ctx.ci.UpdateProgress(progress_msg)

        logger.info("Input:\n" + str(self.input))
        spec = yav_pb2.YavSecretSpec(uuid=self.input.context.secret_uid, key="teamcity_token")
        ts_token = self.ctx.yav.get_secret(spec).secret
        build_type = self.input.build.build_type
        flow_type = self.input.build.flow_type

        headers = {
            "Authorization": "OAuth {}".format(ts_token),
            "Content-Type": "application/xml",
        }
        if flow_type == 'pr':
            props = {'env.PR_NUMBER': self.input.build.arcanum_review_id}
        elif flow_type == 'release':
            props = {'env.RELEASE_BRANCH': self.input.build.release_branch}
        else:
            raise Exception("Unknown flow type")

        request_data = self._get_teamcity_build_xml(build_type, props)
        logger.info("Request data: %s", request_data)
        response = requests.post(TEAMCITY_API_URL + "/app/rest/buildQueue", headers=headers, data=request_data).text
        logger.info("Server response: %s", response)
        root = et.fromstring(response)
        task_url = root.attrib.get("webUrl")
        task_href = root.attrib["href"]
        if not task_url:
            raise Exception("Teamcity build was not started")

        progress_msg.progress = 0.5
        progress_msg.status = ci.TaskletProgress.Status.RUNNING
        progress_msg.url = task_url
        progress_msg.text = 'Build in queue'
        self.ctx.ci.UpdateProgress(progress_msg)

        while True:
            response = requests.get(TEAMCITY_API_URL + task_href, headers=headers).text
            root = et.fromstring(response)
            task_state = root.attrib["state"]
            if task_state == "finished":
                progress_msg.progress = 1.0
                progress_msg.status = ci.TaskletProgress.Status.SUCCESSFUL
                progress_msg.text = 'Build completed, tests are running'
                self.ctx.ci.UpdateProgress(progress_msg)
                self.output.result.msg = 'Ok'
                return
            time.sleep(60.0)
