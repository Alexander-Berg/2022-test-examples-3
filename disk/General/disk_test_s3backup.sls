data:
    s3:
        endpoint: "https+path://s3.mdst.yandex.net"
        access_key_id: {{ salt.yav.get('sec-01ct0ws3z7j6mfr853ct163v0j[access_key_id]') | json }}
        access_secret_key: {{ salt.yav.get('sec-01ct0ws3z7j6mfr853ct163v0j[access_secret_key]') | json }}
