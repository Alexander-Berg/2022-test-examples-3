import time

import pytest
import sys
import yatest
import yatest.common
from yatest.common.network import PortManager


class SerpSummarizer:
    @classmethod
    def setup_class(cls):
        cls.server_bin = yatest.common.binary_path('search/alice/serp_summarizer/runtime/bin/serp_summarizer_server/serp_summarizer_server')
        cls.cli_bin = yatest.common.binary_path('search/alice/serp_summarizer/runtime/bin/serp_summarizer_client_cli/serp_summarizer_client_cli')

        port_manager = PortManager()
        cls.port = port_manager.get_port()

        voc = yatest.common.work_path('data/54.voc')
        model = yatest.common.work_path('data/khr2_m.npz')
        sound_trie = yatest.common.work_path('data/sound.trie')
        cmd = [cls.server_bin, '-c', yatest.common.source_path('search/alice/serp_summarizer/runtime/config/testing.pb.txt')]
        cmd += ['--http-server-params-port', str(cls.port)]
        cmd += ['--summarizer-params-tokenizer-params-naive-tokenizer-params-max-match-bpe-aware-voc-path', voc]
        cmd += ['--summarizer-params-core-params-abstractive-params-input-sequence-params-word-to-id-voc-path', voc]
        cmd += ['--summarizer-params-core-params-abstractive-params-model-params-model-path', model]
        cmd += ['--summarizer-params-postprocess-params-voiced-source-trie-path', sound_trie]
        cls.server_io = yatest.common.execute(cmd, wait=False, stdout=sys.stdout, stderr=sys.stderr, check_sanitizer=False)
        time.sleep(20)

    @classmethod
    def teardown_class(cls):
        if cls.server_io.running:
            cls.server_io.kill()

    @classmethod
    def run_file(cls, request, filename):
        assert cls.server_io.running
        test_filename = '{}_{}.json'.format(request.function.__name__, filename.replace('.', '_'))
        yatest.common.execute(
            f"cat {yatest.common.runtime.work_path(filename)} |\
            {cls.cli_bin} --pp --async --max-retries 10 --do-fail --host localhost:{cls.port} > {test_filename}",
            shell=True, timeout=600)

        return yatest.common.canonical_file(test_filename, local=True)


@pytest.fixture(scope='session')
def summarizer(request):
    SerpSummarizer.setup_class()
    app = SerpSummarizer()
    request.addfinalizer(SerpSummarizer.teardown_class)
    return app


def test_baskette(request, summarizer):
    return summarizer.run_file(request, 'baskette3.jsons')
