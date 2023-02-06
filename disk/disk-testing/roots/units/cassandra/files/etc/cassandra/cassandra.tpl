cluster_name: '[__C_GROUP__]'
num_tokens: 256
hinted_handoff_enabled: true
max_hint_window_in_ms: 10800000 # 3 hours
hinted_handoff_throttle_in_kb: 1024
max_hints_delivery_threads: 2
authenticator: AllowAllAuthenticator
authorizer: AllowAllAuthorizer
permissions_validity_in_ms: 2000
partitioner: org.apache.cassandra.dht.Murmur3Partitioner
data_file_directories:
    - /opt/cassandra/data
commitlog_directory: /opt/cassandra/commitlog
disk_failure_policy: stop
key_cache_size_in_mb: 4096
key_cache_save_period: 14400
row_cache_size_in_mb: 4096
row_cache_save_period: 14400
# row_cache_keys_to_save: 100
saved_caches_directory: /var/lib/cassandra/saved_caches
commitlog_sync: periodic
commitlog_sync_period_in_ms: 10000
commitlog_segment_size_in_mb: 32

seed_provider:
    - class_name: org.apache.cassandra.locator.SimpleSeedProvider
      parameters:
          - seeds: "[__SEEDS__]"

concurrent_reads: 512
concurrent_writes: 512
file_cache_size_in_mb: 512
memtable_total_space_in_mb: 2048
memtable_flush_writers: 4
memtable_flush_queue_size: 4
trickle_fsync: false
trickle_fsync_interval_in_kb: 10240
storage_port: 7000
ssl_storage_port: 7001
listen_address: '[__L_ADDR__]'
start_native_transport: true
native_transport_port: 9042
start_rpc: true
rpc_address: 
rpc_port: 9160
rpc_keepalive: true
rpc_server_type: hsha
thrift_framed_transport_size_in_mb: 15
incremental_backups: false
snapshot_before_compaction: false
auto_snapshot: true
tombstone_warn_threshold: 1000
tombstone_failure_threshold: 100000
column_index_size_in_kb: 512
in_memory_compaction_limit_in_mb: 512
#concurrent_compactors: 1
multithreaded_compaction: false
compaction_throughput_mb_per_sec: 16
compaction_preheat_key_cache: true
# stream_throughput_outbound_megabits_per_sec: 200
read_request_timeout_in_ms: 5000
range_request_timeout_in_ms: 10000
write_request_timeout_in_ms: 2000
cas_contention_timeout_in_ms: 1000
truncate_request_timeout_in_ms: 60000
request_timeout_in_ms: 10000
cross_node_timeout: false
# streaming_socket_timeout_in_ms: 0
# phi_convict_threshold: 8
endpoint_snitch: PropertyFileSnitch
dynamic_snitch_update_interval_in_ms: 100 
dynamic_snitch_reset_interval_in_ms: 600000
dynamic_snitch_badness_threshold: 0.1
request_scheduler: org.apache.cassandra.scheduler.NoScheduler

server_encryption_options:
    internode_encryption: none
    keystore: conf/.keystore
    keystore_password: cassandra
    truststore: conf/.truststore
    truststore_password: cassandra

client_encryption_options:
    enabled: false
    keystore: conf/.keystore
    keystore_password: cassandra
internode_compression: all

inter_dc_tcp_nodelay: false

preheat_kernel_page_cache: false
