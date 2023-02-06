#!/usr/bin/env python

data = [
    {
        "description": "provides yandexuid",
        "timestamp": "1647516189.032",
        "http_user_agent": "Mozilla/5.0 (iPhone; CPU iPhone OS 15_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.0 Mobile/15E148 Safari/604.1",
        "http_cookie": "yandex_gid=35; yandexuid=9295770711498946247; amcuid=2342165011644334751; my=YysCI+AATl0A",
        "request_uri": "/track?service=metrika&id=7819455520341025084&mask=6421519215084040335&ref=https%3A%2F%2Fyandex.ru%2Ftouchsearch",
        "tcp_syn_options": "0204055003030604022812fe1b8eeafc67b92b4904d7321fe2dcfc01010101010101010101010101",
    },
    {
        "description": "provides duid",
        "timestamp": "1647516220.032",
        "http_user_agent": "Mozilla/5.0 (iPhone; CPU iPhone OS 15_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.0 Mobile/15E148 Safari/604.1",
        "http_cookie": "yandex_gid=35; amcuid=2342165011644334751; my=YysCI+AATl0A",
        "request_uri": "/track?service=metrika&id=26015962905770700721&mask=10648223054749307562&ref=https%3A%2F%2Fhhcdn.ru%2Fnposter%2F187334.v1.html",
        "tcp_syn_options": "0204055003030604022812fe1b8eeafc67b92b4904d7321fe2dcfc01010101010101010101010101",
    },
    {
        "description": "provides another duid",
        "timestamp": "1647516220.032",
        "http_user_agent": "Mozilla/5.0 (iPhone; CPU iPhone OS 15_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.0 Mobile/15E148 Safari/604.1",
        "http_cookie": "yandex_gid=35; amcuid=2342165011644334751; my=YysCI+AATl0A",
        "request_uri": "/track?service=metrika&id=26015962905770700722&mask=10648223054749307562&ref=https%3A%2F%2Fzaba.ru%2Fnposter%2F187334.v1.html",
        "tcp_syn_options": "0204055003030604022812fe1b8eeafc67b92b4904d7321fe2dcfc01010101010101010101010101",
    },
    {
        "description": "provides duid for another ext_id",
        "timestamp": "1647516220.032",
        "http_user_agent": "Mozilla/5.0 (iPhone; CPU iPhone OS 15_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.0 Mobile/15E148 Safari/604.1",
        "http_cookie": "yandex_gid=35; amcuid=2342165011644334751; my=YysCI+AATl0A",
        "request_uri": "/track?service=metrika&id=26015962905770700723&mask=10648223054749307562&ref=https%3A%2F%2Flapa.ru%2Fnposter%2F187334.v1.html",
        "tcp_syn_options": "0204055003030604022812fe1b8ffafc67b92b4904d7321fe2dcfc01010101010101010101010101",
    },
    {
        "description": "dropped - inapp useragent",
        "timestamp": "1647516220.032",
        "http_user_agent": "Mozilla/5.0 (iPhone; CPU iPhone OS 15_3_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148",
        "http_cookie": "yandex_gid=35; amcuid=2342165011644334751; my=YysCI+AATl0A",
        "request_uri": "/track?service=metrika&id=22015962905770700724&mask=10648223054749307562&ref=https%3A%2F%2Fhhcdn.ru%2Fnposter%2F187334.v1.html",
        "tcp_syn_options": "0204055003030604022812fe1b8eeafc67b92b4904d7321fe2dcfc01010101010101010101010101",
    },
    {
        "description": "dropped - yastatic.net",
        "timestamp": "1647516220.032",
        "http_user_agent": "Mozilla/5.0 (iPhone; CPU iPhone OS 15_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.0 Mobile/15E148 Safari/604.1",
        "http_cookie": "yandex_gid=35; amcuid=2342165011644334751; my=YysCI+AATl0A",
        "request_uri": "/track?service=metrika&id=23015962905770700725&mask=10648223054749307562&ref=https%3A%2F%2Fyastatic.net%2Fnposter%2F187334.v1.html",
        "tcp_syn_options": "0204055003030604022812fe1b8eeafc67b92b4904d7321fe2dcfc01010101010101010101010101",
    },
    {
        "description": "dropped - no ext_id",
        "timestamp": "1647516220.032",
        "http_user_agent": "Mozilla/5.0 (iPhone; CPU iPhone OS 15_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.0 Mobile/15E148 Safari/604.1",
        "http_cookie": "yandex_gid=35; amcuid=2342165011644334751; my=YysCI+AATl0A",
        "request_uri": "/track?service=metrika&id=26015962905770700726&mask=10648223054749307562&ref=https%3A%2F%2Fhhcdn1.ru%2Fnposter%2F187334.v1.html",
        "tcp_syn_options": "02040550",
    },
    {
        "description": "dropped - no id",
        "timestamp": "1647516220.032",
        "http_user_agent": "Mozilla/5.0 (iPhone; CPU iPhone OS 15_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.0 Mobile/15E148 Safari/604.1",
        "http_cookie": "yandex_gid=35; amcuid=2342165011644334751; my=YysCI+AATl0A",
        "request_uri": "/track?service=metrika&mask=10648223054749307562&ref=https%3A%2F%2Fhhcdn.ru%2Fnposter%2F187334.v1.html",
        "tcp_syn_options": "0204055003030604022812fe1b8eeafc67b92b4904d7321fe2dcfc01010101010101010101010101",
    },
    {
        "description": "dropped - bad mask",
        "timestamp": "1647516220.032",
        "http_user_agent": "Mozilla/5.0 (iPhone; CPU iPhone OS 15_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.0 Mobile/15E148 Safari/604.1",
        "http_cookie": "yandex_gid=35; amcuid=2342165011644334751; my=YysCI+AATl0A",
        "request_uri": "/track?service=metrika&id=26015962905770700727&mask=106482230x4749307562&ref=https%3A%2F%2Fhhcdn.ru%2Fnposter%2F187334.v1.html",
        "tcp_syn_options": "0204055003030604022812fe1b8eeafc67b92b4904d7321fe2dcfc01010101010101010101010101",
    },
    {
        "description": "dropped - no ref",
        "timestamp": "1647516220.032",
        "http_user_agent": "Mozilla/5.0 (iPhone; CPU iPhone OS 15_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.0 Mobile/15E148 Safari/604.1",
        "http_cookie": "yandex_gid=35; amcuid=2342165011644334751; my=YysCI+AATl0A",
        "request_uri": "/track?service=metrika&id=26015962905770700728&mask=10648223054749307562",
        "tcp_syn_options": "0204055003030604022812fe1b8eeafc67b92b4904d7321fe2dcfc01010101010101010101010101",
    },
]

import json

for entry in data:
    print (json.dumps(entry))
