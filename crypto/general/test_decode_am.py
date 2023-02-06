from __future__ import unicode_literals, print_function

import json
import pytest

from crypta.graph.v1.python.matching.device_matching.app_metrica.account_manager_decoder.decoder import (
    decode,
    _decode_aes_ecb,
    _decode_aes_gcm,
)


AM_DATA = [
    {
        "EventName": "AM_System AM info",
        "DeviceID": "068464627d1310a71b24c0d09dd52ef8",
        "data": "vqsqr9Dn7R+dodi\/fXWAgOxhk+Ik9yGsgYVGIOqrz7Xg1rJ2wac4ejoAND8ex5BRUuE1jd8O9wVx+dpPKKWwWMulsYdMPQT\/chcqGNxOotmZLnqv1L+anbFVspc+hcrR4UPcLew4e9olsygVZQ3hn\/AZ+EfzZ4cxECSkOnJHrg4SfBNEB62qYYm9hNCsWKIfPtTz\/79JjD1wXBWH9GTmnGx3oOHX808muioxD+1937SkFgV0x3JGXN4AkQM2A7Gcrj9UHCsyuv5t2EkThdLRLWeHjcAC9qn0E4EkBHjkZxzJ03Jpnmj8B5VyO8fCB\/iCIoh7Zfzgzb2+3CgpEmHLxA==",  # noqa
    },
    {
        "EventName": "AM_System AM info",
        "DeviceID": "1f765af5cbf9eeec832c98ce763e6f18",
        "data": "FIqLYS4yVKl6Xqgg3qOQ9KBDKf7Qf7w3m8+K1DWs2CFxuAOzFQTLWmIVFFHYEl3O5WRDGHHxuOR+0dwSefq2DqanvBQcCPeBcO1hLT6N2L9Sbyuocdjsg+OJ6yedNeB65pflRfLK5PBpN+XqvfSSx03XwK4qKo1Jxl\/QMX16DdiaziEBcFPkow7kdNluQrmw9a60QIR8EaKK5aPXoyyD6B2i\/H4gf0BtDCZlg6mFXK204rInqxl9E2HVlIXYsSjrs84+Ypvs+LZyhwbApU97MnJa4XI70IFovbt19ks39lw9nhFXEkrHN6UHIsqmJt\/3rWBBtVKkd0aCuT\/M\/\/LSwg==",  # noqa
    },
    {
        "EventName": "AM_System AM info",
        "DeviceID": "2cbf9d72178d586f797e34c4aa4e6a45",
        "data": "pacAqVW5IFWIumhpetMtNZn30nCuAZtmXXOKl26zqbeAVmBtq1CICYy4toz8EX4+kVnJcC2+Cq8YpJlfNyT8NG4ZBOAK1E2c0rvKT1PUFzINOEJDE7pQlHPShaWwcVPf48KN+EvBJiimNwWuzb81AGP8M9nZ8\/ZluRI1tzA3LwYV0nUx2hrrkp7s98ygB8bOkmtYs5ep9IZYN7XlAYPUkcFNbfUeJ2PDWjAvx4nAUsg7tsA3wqTYJb3I9m3ddZeNNvZ9h6TaRrrYag31VJ+y4LobfDQHrMP9FYYj6IsCsl1RjZvRHZ59GOYGbZbxVJ4v13EZ+igKUJO7\/8mHTbYEaQ==",  # noqa
    },
    {
        "EventName": "AM_System AM info",
        "DeviceID": "54f4d3d8dca6050f549d31b71f11955b",
        "data": "N0TWEt\/ehKjj\/5zzhrO0YOWRY41u7q3nelZNR1Aj1InNsRmMptHcU14bgnXfYeYte+D\/tOvSO9jhJZbrnuOJ6qg\/5i0dPgWB5X9Eb1DfKcds54zjFsBD0uh828unyeWcO\/Y+yTTJPF\/wSA7eXYToUNq24WG4wKJx13x5btK1GYcQqap46+mTF2n5seSawLcRkqeQ5RVFCPHYNHkdo+WSxO9IuON1OTyRBDl4frIaEtX1KpA3Rv+GYnSVqkFFFLj3Gdsz0u62V8T2b2nH1I7UMxEKgjTIB+01mrS5PnynW2idG\/hcT7RmH6ZPa4\/HUueGOsJGgBjUB3aiO\/S38HPcJRcKpGrDqv3eD1cs7Rcj8wqe5Ir+UVWHUXCKOKxYv+9rqEJCWXBCOFSlddxh6oQ6D5heYc2rXuN5nLdICXNGwqJoHN4l3Mr0Ku1xsnyi6PIhol0\/\/b4kXSyQ768s+\/YaJntO2dvFxKBTywmYpNnvdaUV6zccGsNfvGdhTPE8oxP71dC6KsUQl20l3xAn6Xm2TN\/52QVGFHQzRAzYs07ulDDIvGNT9dUbpipyqvOF\/jhn5noOdQb5SI\/Fu10t72WFwEJshcF9AR2vwALMK4DWCceLJUVa7oVVu3uq9H6iDGsG\/zAtckfDJOEaJ1cHYrB7w9lDWdu94zoStyb9jUkNDEs=",  # noqa
    },
    {
        "EventName": "AM_System AM info",
        "DeviceID": "54f4d3d8dca6050f549d31b71f11955b",
        "data": "sjD5FwynEx3gLKG+gG+P4YF6SgjBWH9b6V0qqrBjUolzCtvdGCdsUqLLRWaAoitBIE5iif5VgJ6RQs7ESRVWkX5XEQqIDH6xjoF1P8SFwLAX+QYF+aww17v4Z9XJXaZKmllbdO6WkIuTg7Y4\/Ak7pLg00+iV3fbte7RsQp9q0x+px6vTNpYkizFkiStkMT7h+mc+\/stsIk+ZWyhzkl55HljGfEMrnfFkVkeX\/gobsqTUkHCh0zChAMhWH0Nb4+QXmgEaI1DU519+2j7InLFZD8JjEpsXDnjnXiA+8ew+muOeTDZm4rityXU0mQLHtpD1rYuAipZETwahbgg\/qBycHbfO6ef9d4S9vLF8DUTvMvt3HKerEeqpfBm3y\/CCIo3u\/6ve7H447NIOzYJ51jJUjAAfMa1aO1Q7\/+ctYgS8ijbgiZGk\/dbwkaVTYdefFG57Eg6vBtZIQpYUEMeLM0MfjBRSJCzjRGr0VSwgWem6b1yUiN8TEEwqgGV1Y46LxODca3u\/ltkirhXqjfFEMQUkvPfyLt2pPTosiW007ADZ3CDeX+qJpJgGLCURGUTYK7Rfd\/aHne\/sstPTSpTdOaLe0d1x4W4OptYKxCkPy328oCQCYhm\/KZ9K4+\/vXmWKWlyNDv6xMYsFxGX3qunKM2MERRVD4n0RLNksuiukql76ynM=",  # noqa
    },
    {
        "EventName": "AM_System AM info v3",
        "DeviceID": "277e284eb154d3f2a33fac80b9264fab",
        "data": "KtlDFpFgOCK40LnLtcRAjAKbvGb4wj3PnAu12pn\/3G6ldIy6ncpNEjyCOrud0NgD7af2IynRefJ5nAGwdboJAO\/wcBkp+kxkktSZFNELPGqAE+ry7ALexrdz2CZqEeBqwPgwWrkCsCYUcLESUlWheMUcpeiTS\/W3Yl8XiNFjov\/0pCAu8jRK4GFb1mT4Enk5psAR0WdgR9cDWKZHSBKODYSlvFivlYtidIcT09cwy2hSyPzE2eXFFzjlDIhUJHKh8rVpjNqfB5j9XIK5pBdZ0E1l5yOYzYr9njo15cuUA7m+Fink05eqIk5SFm+QlLdn7rquaR\/pOVc+yWfo5dVC6w==",  # noqa
    },
    {
        "EventName": "AM_System AM info v3",
        "DeviceID": "52b09fe1564675e95369367cad56f534",
        "data": "B9AoomPpr3Bb0vIaI0hwSVmurreiDZvXIePUobLmSuG0p8QLUVQKO3DjqTyJYBsA1I+nWldBEuIotennWiRJb4PkiHkoLofWjKKAEx0aBUW2Rjq5orUYkCssol9RWREAAnvT\/MW9UBq83D1nNQpUhe8iklC2W7LF9urEQ4tR8hLUF8UR2f0lPcCQdhF4j6O+YYcOolrT5rx6z6lh7CphOWj\/KHsOIukBW\/sBrKGRmTJl7qlIF3Gtftla0MEDWNdp+wz150j6+75y4VXz\/o40ogJHdgXM8W1Da7mMF7YbKrilxl5tqy9jw3QsA61p3vpGS0mW3i1CrTb9ZRs5sxxmcQ==",  # noqa
    },
    {
        "EventName": "AM_System AM info v3",
        "DeviceID": "5d74d8701b392985ca393d0af6a53d02",
        "data": "R57+QAbIQdmSfBrZf7uoOfhMeqARzljWjmFCJRcI06P4J5AAeKAqtzYuuRDXs8CrYkiWVb1dT7MsF3xsJlBTgrxKAtuP9gQ3ercyCwM6hMTbbmo3qUcgUypJYKgj7s7V0z\/WNY2i7WvyhdDPPTfOPOy2LunC\/nGLG\/hfXAX8UfF8TP8UpTLRCz+c6vsAJ\/p+tghIPohb7nddnLa7fNirb8oIlBjHsNYUPqUBsKff+FX0gqPhIWo2eD90oE+urhZkIdqsDJtbW2DaNMSVqExu0CaiH7+Rdf\/YI5DLiE\/rRVCh4svVP1Dzv9sItW\/mPHigDB8h7PVUeU7mosppLi14YA==",  # noqa
    },
    {
        "EventName": "AM_System AM info v3",
        "DeviceID": "94656c7068afa30b6f8c3d57684bc74d",
        "data": "TQ+guxNmuHhHoGZCQglTlWUXd1GvNHPXZx8kIcPuROkT2qki0XYYSt8rDmlivkAHZQbm3Qc3Zqhg5h7hbP13zzGjr\/MpTRUfjzeEpQDXJHFytItp6\/UDfX6+JoZawmNf3N+iel\/X81afuM2t6dNprz38otttAKrOCAFRgsQRNJtEKQ+r1mpEM2MTvmtdE1VfY2esP7ttXUb7XtBd+rQG47HIfromCcVpPaudJJE6Z4JxnyJVDh9zXVNW77zrkl6VvX2JlLMJHv7oVaBsC3+epSjoxBKJf\/1gOSLi797D+bkExMqQsSmc5lWzWYK4jQ60rv0KxowFX1JqN6GIIZuJOg==",  # noqa
    },
    {
        "EventName": "AM_System AM info v3",
        "DeviceID": "bfb3b4b9dc9135e51032fc8fc58ff072",
        "data": "Gd4hE2lRb9ZiJvd19dFnbBf1HFgOUKbAY0WAFsbWC96c4YBRc6\/1aixHhPZZDCyJCiaMzBRMKGvdfEClutZK8xh6F8fHsmKEdu+fqtLEl1yc3EF2D3\/EOagVeijMP2mfll02mkb3Fi4alo04DVL4XUYa15SYsL9WyP8YfEgetBOqsfVwNf9ek+s8Q8Ml3UmuBmgqGUL+AJbn+VfnhGfVtiY4gHLm9xjKP8WegqCAj1OAKXn4aNdYQ\/AOmRintfcXvCla6F9\/Pq4KrXi9Ga2+cyaiky4h9NU9x70c\/\/+Go4ZqG8X1lslWtDi0P7tb\/e01FPK0BZhdSic7zQgKya5eOA==",  # noqa
    },
]


@pytest.mark.parametrize("index", xrange(len(AM_DATA)))
def test_decode_am(index):
    data = AM_DATA[index]["data"]
    device_id = AM_DATA[index]["DeviceID"]

    decoded = decode(data, aes_pass=device_id)
    result = json.loads(decoded)
    assert isinstance(result["a"], list)
    return result


@pytest.mark.parametrize(
    "index",
    map(
        lambda pair: pair[1],
        filter(lambda pair: pair[0]["EventName"] == "AM_System AM info", zip(AM_DATA, xrange(len(AM_DATA)))),
    ),
)
def test_decode_am_v1(index):
    data = AM_DATA[index]["data"]
    device_id = AM_DATA[index]["DeviceID"]

    result = json.loads(_decode_aes_ecb(data, aes_pass=device_id))
    assert isinstance(result["a"], list)
    return result


@pytest.mark.parametrize(
    "index",
    map(
        lambda pair: pair[1],
        filter(lambda pair: pair[0]["EventName"] == "AM_System AM info v3", zip(AM_DATA, xrange(len(AM_DATA)))),
    ),
)
def test_decode_am_v23(index):
    data = AM_DATA[index]["data"]
    device_id = AM_DATA[index]["DeviceID"]

    result = json.loads(_decode_aes_gcm(data, aes_pass=device_id))
    assert isinstance(result["a"], list)
    return result


def test_decode_am_sp():
    data = (
        "aUld0atUU9Tkha0s4mDR04XLEQQB/1zclTCTVf73f90SAqquSdoLuklaNASQz8a9U9ZX2low+sgJiUH8Gm88OEK"
        "VEwu51LtbgzSHG6eo+3tIb61l6hxq64GL8DQvX7fWXdcKLMvDBSvXRYFzpqLl7f0LHKu2vSrp8CnHCKrwL7wOtw"
        "Nbl4p7r2Q94wH4ApqFohfXX5b4/ZxzwM+NT2JyTp/tt2midiaL3RN9/Yh07+ViflUJMyDrup55Ac0mp8g8g75KG"
        "4TskKoV8ryz8/W3FHNrTK66oQdW2VAwSGn6r3KYDLqUaIYX8OkbzsdOZ3SkeR26GkFw16nuJ1QwzW7Jew=="
    )
    device_id = "password123"

    result = _decode_aes_gcm(data, aes_pass=device_id)
    assert result == "Hello here"
