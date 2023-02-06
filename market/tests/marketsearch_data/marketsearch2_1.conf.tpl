[general]
heat_search_delta_threshold = 400
download_dir = %%WORKDIR%%/marketsearch
preport_log = %%WORKDIR%%/preport.log
httpsearch = %%WORKDIR%%/httpsearch
checksearch = %%WORKDIR%%/checksearch_fail
report_data = %%WORKDIR%%/dists/report-data
timetail = %%WORKDIR%%/timetail
logfile = %%WORKDIR%%/disk2mem.log
reportconf_dir = %%WORKDIR%%/dists/report-data
reportconf_gen = %%WORKDIR%%/generate

qbids_updator      = echo
report_updator     = echo
qbids_download_dir = %%WORKDIR%%/delta_download
qbids_dir          = %%WORKDIR%%/delta
qbids_snapshot_dir = %%WORKDIR%%/snapshot
index_dir          = %%WORKDIR%%/index
reload_qbid_lock   = %%WORKDIR%%/qbid.reload.lock
reload_qindex_lock = %%WORKDIR%%/qindex.reload.lock
reload_lock        = %%WORKDIR%%/marketsearch.reload.lock


qindex_dir          = %%WORKDIR%%/qindex
report_updater      = echo
report_data_dir     = %%WORKDIR%%/report-data


[dists]
report-data = %%WORKDIR%%/dists/report-data
test1 = %%WORKDIR%%/dists/test1
test2 = %%WORKDIR%%/dists/test2

[marketsearch]
lastop = %%WORKDIR%%/backctld.marketsearch.lop
