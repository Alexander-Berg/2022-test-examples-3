import random
import string

from crypta.lib.python.identifiers import generic_id
from crypta.lib.python.identifiers.identifiers import Email, Phone, Login, IdfaGaid, Puid


def random_name(N):
    return ''.join(
        random.choice(string.ascii_uppercase + string.digits) for _ in range(N)
    )


def random_related_goals():
    return [str(random.randint(10000, 99999)) for _ in xrange(3)]


generic_id.set_random_seed(42)
ID_COUNT = 1000
IDFAS_GAIDS = [IdfaGaid.next() for _ in xrange(ID_COUNT)]
PHONES = [Phone.next() for _ in xrange(ID_COUNT)]
EMAILS = [Email.next() for _ in xrange(ID_COUNT)]
JUNK = [Login.next() for _ in xrange(2)]
PUIDS = [Puid.next() for _ in xrange(ID_COUNT)]

OTHER_COUNT = 1000
YANDEXUIDS = [str(1200000001500000000 + i) for i in xrange(ID_COUNT)]
OTHER_YANDEXUIDS = [str(1300000001500000000 + i) for i in xrange(OTHER_COUNT)]
JUNK_YANDEXUIDS = [str(2400000001500000000 + i) for i in xrange(10)]
