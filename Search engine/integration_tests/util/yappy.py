import requests
from requests.adapters import HTTPAdapter
from requests.packages.urllib3.util.retry import Retry

from copy import deepcopy

from search.martylib.grpc_utils import get_channel
from search.martylib.protobuf_utils.patch import patch_enums
from search.stoker.services.stoker import services
from search.stoker.proto.structures.record_pb2 import Record

from util.const import SEARCH


def get_beta_params(beta_url):
    patch_enums()

    beta_name = beta_url.split('.')[0]
    if beta_name == "hamster":
        return list(), dict()

    params, headers = list(), dict()
    chanel = get_channel('stoker.z.yandex-team.ru:80',
                         authority='stoker.z.yandex-team.ru')
    client = services.ModelClient(chanel,
                                  ensure_compatible_iss_configuration=False)

    response = client.get_record(Record(type=Record.Type.TAGGED, key=beta_name))
    for p in response.content.cgi:
        name, value = p.split("=", 1)
        params.append((name, value))

    return params, headers


def requests_retry_session(retries=3,
                           backoff_factor=0.3,
                           status_forcelist=(500, 502, 504),
                           session=None):
    session = session or requests.Session()
    retry = Retry(total=retries,
                  read=retries,
                  connect=retries,
                  backoff_factor=backoff_factor,
                  status_forcelist=status_forcelist)
    adapter = HTTPAdapter(max_retries=retry)
    session.mount('http://', adapter)
    session.mount('https://', adapter)
    return session


def do_request(beta_url, handler=SEARCH, text="test", timeout=5, headers=None, params=None):
    assert type(headers) in (type(None), dict)
    assert type(params) in (type(None), list)

    beta_params, beta_headers = deepcopy(get_beta_params(beta_url))
    if headers is not None:
        beta_headers.update(headers)
    if params is not None:
        beta_params.extend(params)

    assert "text" not in {k for k, v in beta_params}
    beta_params.append(("text", text))

    url = "https://{beta_url}{handler}".format(beta_url=beta_url, handler=handler)

    return requests_retry_session().get(url, params=beta_params, headers=beta_headers, timeout=timeout)
