import yatest
import yatest.common.network

from crypta.lib.python import templater
from crypta.cm.services.common.test_utils.rt_duid_uploader import RtDuidUploader


class RtCmDuidUploader(RtDuidUploader):
    bin_path = "crypta/cm/services/rt_duid_uploader/rt_cm_duid_uploader/bin/crypta-cm-rt-duid-uploader"
    app_config_template_path = "crypta/cm/services/rt_duid_uploader/rt_cm_duid_uploader/bundle/templates/config.yaml"
    factory_config_template_path = "crypta/cm/services/rt_duid_uploader/rt_cm_duid_uploader/bundle/templates/factory_config.yaml"

    def __init__(self, url, tvm_api, src_tvm_id, dst_tvm_id, *args, **kwargs):
        super(RtCmDuidUploader, self).__init__(*args, **kwargs)
        self.url = url
        self.src_tvm_id = src_tvm_id
        self.dst_tvm_id = dst_tvm_id
        self.env["TVM_SECRET"] = tvm_api.get_secret(src_tvm_id)

    def _render_factory_config(self):
        templater.render_file(
            yatest.common.source_path(self.factory_config_template_path),
            self.factory_config_path,
            {
                "url": self.url,
                "source_tvm_id": self.src_tvm_id,
                "destination_tvm_id": self.dst_tvm_id,
            },
        )
