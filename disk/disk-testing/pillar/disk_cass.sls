
cluster : disk_cass

include:
  - units.cassandra

# =======================

disk_cass-files:
  - /etc/monitoring/unispace.conf

disk_cass-additional_pkgs:
  - psmisc
  - util-linux

