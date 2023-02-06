data:
    readahead_buffer_bytes:
        md2: 32
    use_walg: True
    use_barman: False

gpg-yav-secrets: {{ salt.yav.get('sec-01dpk49pq2v5v6njxb7665ckr5') | json }}
