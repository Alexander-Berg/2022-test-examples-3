data:
    readahead_buffer_bytes:
        md2: 32
    use_walg: True
    use_barman: False
    config:
        shared_buffers: 32GB
        effective_cache_size: 32GB
        maintenance_work_mem: 4GB
        max_connections: 2000
    sysctl:
        vm.nr_hugepages: 34816


gpg-yav-secrets: {{ salt.yav.get('sec-01dz6qtb4jssrqejxgvqpykn6f') | json }}

