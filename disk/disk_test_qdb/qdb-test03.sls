data:
    readahead_buffer_bytes:
        md2: 32
    use_walg: True
    use_barman: False
    config:
        shared_buffers: 124GB
        effective_cache_size: 64GB
        maintenance_work_mem: 4GB
        max_connections: 2000

gpg-yav-secrets: {{ salt.yav.get('sec-01dpjn1ty9859fpf31epcyrsaj') | json }}
