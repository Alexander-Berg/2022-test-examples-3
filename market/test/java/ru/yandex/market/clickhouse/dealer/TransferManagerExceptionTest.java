package ru.yandex.market.clickhouse.dealer;

import org.junit.Assert;
import org.junit.Test;

public class TransferManagerExceptionTest {

    @Test
    public void emptyMessageTest() {
        check("", new TransferManagerException(""));
        check("", new TransferManagerException(null));
    }

    @Test
    public void noPatternMatchTest() {
        check("", new TransferManagerException("no pattern message"));
    }

    @Test
    public void whatMessageTest() {
        check(
            String.format(
                TransferManagerException.TM_ERROR_REASON_MESSAGE,
                "transfer_manager/copy_yt_to_clickhouse/libs/row_translation/yt_helpers.cpp:27:" +
                    " required column not found in yt row: \"shows\""
            ),
            new TransferManagerException(
                "0x107800840\\\\n " +
                    "   what() -> \"transfer_manager/copy_yt_to_clickhouse/libs/row_translation" +
                    "/yt_helpers.cpp:27: " +
                    "required column not found in yt row: \"shows\"\"\\\\n type -> " +
                    "yexception\\\\n/bin/bash: line 1:"
            )
        );
    }

    @Test
    public void columnDecoderMessageTest() {
        check(
            String.format(
                TransferManagerException.TM_ERROR_REASON_MESSAGE,
                "transfer_manager/copy_yt_to_clickhouse/libs/row_translation/" +
                    "ch_column_decoder.cpp:23: cannot decode value of ClickHouse type Int32 from " +
                    "Yt value 490922u (uint64_node) in column \"feed_id\""
            ),
            new TransferManagerException(
                "-manager-22350.gencfg-c.yandex.net on 2020-02-13T18:03:25.922732Z    \\n    stderrs         " +
                    "[{\\'host\\': \\'sas5-0569-tablet-node-hahn.sas.yp-c.yandex.net:9012\\', " +
                    "\\'stderr\\': \\'" +
                    "uncaught exception:\\\\n    address -> 0x107801e00\\\\n    what() -> " +
                    "\"transfer_manager/" +
                    "copy_yt_to_clickhouse/libs/row_translation/ch_column_decoder.cpp:23: cannot decode " +
                    "value of " +
                    "ClickHouse type Int32 from Yt value 490922u (uint64...message truncated...    \\n   " +
                    " url    " +
                    "         http://hahn.yt.yandex.net/?page=operation&mode=detail" +
                    "&id=3c225842-afbd7cb7-3fe03e8-41b4a2ad&tab=details    \\n    state           failed " +
                    "   \\n " +
                    "   id              3c225842-afbd7cb7-3fe03e8-41b4a2ad\\nFailed jobs limit exceeded  " +
                    "  \\n " +
                    "   origin          sas4-5568-controller-agent-hahn.sas.yp-c.yandex.net on" +
                    " 2020-02-13T18:03:18.494368Z (pid 105, tid 25ec0da53705dd06, fid fff8c104aebc2386)  " +
                    "  \\n  " +
                    "  max_failed_job_count 5\\nUser job failed    \\n    code            1205    \\n    " +
                    "origin   " +
                    "       sas5-0569-tablet-node-hahn.sas.yp-c.yandex.net on 2020-02-13T18:03:00.107168Z" +
                    " (pid 597589, tid 7d221c5c93120425, fid ffff05e667a45496)    \\n    stderr          " +
                    "uncaught exception:\\\\n    address -> 0x107801e00\\\\n    what() -> " +
                    "\"transfer_manager/" +
                    "copy_yt_to_clickhouse/libs/row_translation/ch_column_decoder.cpp:23: cannot decode " +
                    "value " +
                    "of ClickHouse type Int32 from Yt value 490922u (uint64_node) in column \"feed_id\": " +
                    "(yexception) transfer_manager/copy_yt_to_clickho...message truncated...\\nProcess " +
                    "exited " +
                    "with code 134    \\n    code            10000    \\n    origin          (unknown) " +
                    "on 2020-02-13T18:02:51.866130Z (pid 2, tid d3826f33e841c81d, fid 0)    \\n    " +
                    "exit_code " +
                    "      134\\n'', code=1}\n"
            )
        );
    }

    @Test
    public void failedJobLinkTest() {
        check(
            String.format(
                TransferManagerException.TM_CLUSTER_JOB_ERROR_MESSAGE, "vanga", "cd0f3f40-acadc749-3f603e8" +
                    "-27c6b86e"
            ),
            new TransferManagerException("raise error\\nYtOperationFailedError:" +
                " Operation cd0f3f40-acadc749-3f603e8-27c6b86e failed\\n    Failed jobs limit exceeded\\n    " +
                "    " +
                "User job failed\\n            Process exited with code 1\\n    User job failed\\n        " +
                "Process exited with code 1\\n\\n***** Details:\\nOperation " +
                "cd0f3f40-acadc749-3f603e8-27c6b86e failed" +
                "    \\n    origin          man1-7351-man-yt-transfer-manager-27570.gencfg-c.yandex.net " +
                "on 2020-09-24T11:39:20.828047Z    \\n    stderrs         [{\\'host\\': \\'" +
                "sas4-8921-node-vanga.sas.yp-c.yandex.net:9012\\', \\'stderr\\': \\'2020-09-24T14:38:30+0300 " +
                "[INFO]" +
                " -- Th-Main -- program version (revision number): 7100579\\\\n2020-09-24T14:38:30+0300 " +
                "[INFO]" +
                " -- Th-Main -- lock acquired on shard \"//tmp/copy_yt_to_clickhouse/tasks/" +
                "robot-mrk-ch-dealer/MDB:market_mbi_c...message truncated...    \\n    url             " +
                "http://vanga.yt.yandex.net/?page=operation&mode=detail&id=cd0f3f40-acadc749-3f603e8-27c6b86e" +
                "&" +
                "tab=details    \\n    state           failed    \\n    id              " +
                "cd0f3f40-acadc749-3f603e8-" +
                "27c6b86e\\nFailed jobs limit exceeded    \\n    code            215    \\n    origin        " +
                "  " +
                "sas5-4282-controller-agent-vanga.sas.yp-c.yandex.net on 2020-09-24T11:39:18.257672Z (pid 2, " +
                "tid 1421320131085595, fid fffe07a949107a50)    \\n    max_failed_job_count 1\\nUser job " +
                "failed " +
                "   \\n    code            1205\\nProcess exited with code 1    \\n    code            10000 " +
                "   \\n  " +
                " exit_code       1\\nUser job failed    \\n    code            1205    \\n    stderr        " +
                "  " +
                "2020-09-24T14:38:30+0300 [INFO] -- Th-Main -- program version (revision number): " +
                "7100579\\\\n" +
                "2020-09-24T14:38:30+0300 [INFO] -- Th-Main -- lock acquired on shard \"//tmp/copy_yt_to_clickhouse/" +
                "tasks/robot-mrk-ch-dealer/MDB:market_mbi_clickhouse.dealer_temp.mbi__analyst_orders_tmp_lr" +
                ".89438a19-3232-4fb6-a4...message truncated...\\nProcess exited with code 1    \\n    code          " +
                "10000    \\n    exit_code       1\\n'', code=1}")
        );
    }

    private void check(String expectedMessage, TransferManagerException tmException) {
        Assert.assertEquals(expectedMessage, tmException.getMonitoringMessage());
    }
}
