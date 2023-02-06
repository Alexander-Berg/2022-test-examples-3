[general]
heat_search_delta_threshold = 400
download_dir = %%WORKDIR%%/marketsearch
preport_log = %%WORKDIR%%/preport.log
httpsearch = %%WORKDIR%%/httpsearch
checksearch = %%WORKDIR%%/checksearch
report_data = %%WORKDIR%%/dists/report-data
timetail = %%WORKDIR%%/timetail
logfile = %%WORKDIR%%/disk2mem.log
reportconf_dir = %%WORKDIR%%/dists/report-data
reportconf_gen = %%WORKDIR%%/generate
iptruler = true

qbids_updator      = echo
report_updator     = echo
qbids_download_dir = %%WORKDIR%%/delta_download
qbids_dir          = %%WORKDIR%%/delta
qbids_snapshot_dir = %%WORKDIR%%/snapshot
index_dir          = %%WORKDIR%%/index
reload_qbid_lock   = %%WORKDIR%%/qbid.reload.lock
reload_qindex_lock = %%WORKDIR%%/qindex.reload.lock
qpipe_update_lock  = %%WORKDIR%%/qpipe-update.lock
reload_lock        = %%WORKDIR%%/marketsearch.reload.lock


qindex_dir          = %%WORKDIR%%/qindex
report_updater      = echo
report_data_dir     = %%WORKDIR%%/report-data

force_report_restart_file = %%WORKDIR%%/report_force_restart

fqdn_config_path = %%WORKDIR%%/fqdn.json

[dists]
report-data = %%WORKDIR%%/dists/report-data
test1 = %%WORKDIR%%/dists/test1
test2 = %%WORKDIR%%/dists/test2

[marketsearch]
lastop = %%WORKDIR%%/backctld.marketsearch.lop
