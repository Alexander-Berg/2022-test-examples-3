import yatest
import logging
import os
import shutil

import market.idx.pylibrary.mindexer_core.ctr.ctr as ctr

log = logging.getLogger('')


class CtrTsWriter(object):
    def __init__(self, recent_ctrctrdynamic_dir, recent_mstat_experiments_dir, recent_views_orders_dir,
                 recent_ctrincuts_dir, dst_dir):
        self.recent_ctrctrdynamic_dir = recent_ctrctrdynamic_dir
        self.recent_mstat_experiments_dir = recent_mstat_experiments_dir
        self.recent_views_orders_dir = recent_views_orders_dir
        self.recent_ctrincuts_dir = recent_ctrincuts_dir
        self.dst_dir = dst_dir

        self._waremd5_to_ts = {}
        self._next_ts = 1

    def _get_ts(self, waremd5):
        ts = self._waremd5_to_ts.get(waremd5)
        if ts is not None:
            return ts

        ts = self._next_ts
        self._next_ts += 1
        self._waremd5_to_ts[waremd5] = ts
        return ts

    def _convert_ctr_ts(self):
        with open(os.path.join(self.recent_mstat_experiments_dir, "waremd5_ctr_per_ware_md5")) as fn:
            with open(os.path.join(self.dst_dir, "ctr-ts.db"), "w") as out_fn:
                for line in fn:
                    parts = line.split("\t")
                    waremd5 = parts[0]
                    parts[0] = str(self._get_ts(waremd5))
                    out_fn.write("\t".join(parts))

    def _convert_common_wmd5_file(self, src_filename, dst_filename):
        src = os.path.join(self.recent_ctrctrdynamic_dir, src_filename)
        dst = os.path.join(self.dst_dir, dst_filename)
        log.info("[test] Generate fake ts file '%s' -> '%s'", src, dst)

        with open(src) as fn:
            with open(dst, 'w') as out_fn:
                for line in fn:
                    parts = line.split("\t")
                    waremd5 = parts[1]
                    parts[1] = str(self._get_ts(waremd5))
                    out_fn.write("\t".join(parts))

    def convert(self):
        log.info("[test] Generate fake ctr-ts.db")
        self._convert_ctr_ts()

        self._convert_common_wmd5_file("msearch_wmd5_query_ctr", "msearch-ts-query-ctr.db")
        self._convert_common_wmd5_file("msearch_wmd5_normalized_to_lower_query_ctr",
                                       "msearch-ts-normalized_to_lower_query-ctr.db")
        self._convert_common_wmd5_file("msearch_wmd5_normalized_to_lower_and_sorted_query_ctr",
                                       "msearch-ts-normalized_to_lower_and_sorted_query-ctr.db")
        self._convert_common_wmd5_file("msearch_wmd5_normalized_by_dnorm_query_ctr",
                                       "msearch-ts-normalized_by_dnorm_query-ctr.db")
        self._convert_common_wmd5_file("msearch_wmd5_normalized_by_synnorm_query_ctr",
                                       "msearch-ts-normalized_by_synnorm_query-ctr.db")

        with open(os.path.join(self.dst_dir, 'premium-ts-query-ctr.db'), "w"):
            pass


def test_ctr():
    biggen_input_dir = yatest.common.output_path("input")

    recent_ctrctrdynamic_dir = yatest.common.source_path("market/idx/mir/data/getter/ctrdynamic/recent")
    recent_mstat_experiments_dir = yatest.common.source_path("market/idx/mir/data/getter/mstat_experiments/recent")
    recent_views_orders_dir = yatest.common.source_path("market/idx/mir/data/getter/views_orders/recent")
    recent_ctrincuts_dir = yatest.common.source_path("market/idx/mir/data/getter/ctrincuts/recent")
    shutil.copytree(recent_ctrctrdynamic_dir, os.path.join(biggen_input_dir, "getter/ctrdynamic"))
    shutil.copytree(recent_mstat_experiments_dir, os.path.join(biggen_input_dir, "getter/mstat_experiments"))
    shutil.copytree(recent_views_orders_dir, os.path.join(biggen_input_dir, "getter/views_orders"))
    shutil.copytree(recent_ctrincuts_dir, os.path.join(biggen_input_dir, "getter/ctrincuts"))

    ctr_report_data_dst = yatest.common.output_path("report_data_dst")
    ctr_mmap_dst = yatest.common.output_path("mmap_dst")
    os.makedirs(ctr_report_data_dst)
    os.makedirs(ctr_mmap_dst)

    ctr.link_ctr_for_report_from_yt(biggen_input_dir, ctr_report_data_dst, log)

    ctr_ts_writer = CtrTsWriter(recent_ctrctrdynamic_dir, recent_mstat_experiments_dir, recent_views_orders_dir,
                                recent_ctrincuts_dir, ctr_report_data_dst)
    ctr_ts_writer.convert()

    ctr_converter_bin = yatest.common.build_path("market/tools/ctr-to-mms-converter/ctr-to-mms-converter")
    yatest.common.execute([ctr_converter_bin, ctr_report_data_dst, ctr_mmap_dst])

    mmapviewer_bin = yatest.common.build_path("market/tools/mmapviewer/mmapviewer")
    return yatest.common.canonical_execute([mmapviewer_bin, os.path.join(ctr_mmap_dst, "ctr.mmap")])
