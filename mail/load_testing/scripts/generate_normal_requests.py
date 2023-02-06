import json
import itertools
import random
import string

from io import StringIO
from lxml import etree
from random import randint

from mail.template_master.load_testing.scripts.lib.ammo_format_converter import AmmoFormatConverter, HTTPRequestArguments
from mail.template_master.load_testing.scripts.lib.db_adaptor import DBAdaptor, YcDBConnectionProvider
from mail.template_master.load_testing.scripts.lib.util import decode_bytes, get_logger, prepare_db_auth_data, write_file
from mail.template_master.load_testing.scripts.constants import RequestTypes
from mail.template_master.load_testing.scripts.util import get_cmd_args

params = {
    'cluster': 'mdb4nraqr3jsjddc2lif',
    'dbname': 'sherlockdb_pg',
    'password': None,
    'sslrootcert_path': None,
    'user': 'sherlock',
    'vault_secret_version': 'ver-01e180q8ns0exk5qxrswvw9ne8',
}


class RouteRequestBuilder(object):
    def _htmlize(self, html):
        """
        with the `recover` option this thing will try to fix given html
        for example
            <div>
                <a href='example.com'>foo</a>
        will be turned into
            <html>
                <body>
                    <div>
                        <a href="example.com">foo</a>
                    </div>
                </body>
            </html>
        """
        parser = etree.HTMLParser(recover=True)
        tree = etree.parse(StringIO(html), parser)
        return etree.tostring(tree.getroot(), pretty_print=True, method='html')

    def _get_random_string(self, length=10):
        alphabet = string.ascii_lowercase
        return ''.join(random.choice(alphabet) for i in range(length))

    def _generate_request(self, html):
        request = {
            'from': 'template-master-load-testing@yandex-team.ru',
            'html': f'{html}',
            'queueId': f'{self._get_random_string()}',
            'subject': f'{self._get_random_string()}',
            'uids': [f'{str(randint(1e15, 1e18))}']
        }
        return json.dumps(request)

    def build_request(self, db_row):
        """
        1. concatenates all the token lists into a single list
        2. shuffles them (in order to ensure that the resulting html will not
           be found in the db on the first step of the /route handler work)
        3. concatenates them into a single string
        """
        # each template is stored as list of lists of strings
        unnest = lambda iterable: itertools.chain.from_iterable(iterable)
        lst = list(unnest(unnest(db_row)))
        random.shuffle(lst)
        html = decode_bytes(self._htmlize(' '.join(lst)))
        return self._generate_request(html)


class RouteRequestArgumentsProvider(object):
    def get_query(self, limit: int):
        return f"""
            SELECT
                chunks
            FROM
                template_bodies
            ORDER BY
                random()
            LIMIT
                {limit};"""

    def get_request_builder(self):
        return RouteRequestBuilder().build_request

    def get_http_request_arguments_builder(self):
        return lambda request: HTTPRequestArguments(body=request, params=None)


class FindTemplateRequestArgumentsProvider(object):
    def get_query(self, limit: int):
        return f"""
            SELECT
                stable_sign
            FROM
                template_bodies
            ORDER BY
                random()
            LIMIT
                {limit};"""

    def get_request_builder(self):
        def f(row):
            assert len(row) == 1
            return row[0]

        return f

    def get_http_request_arguments_builder(self):
        return lambda request: HTTPRequestArguments(
            body=None, params={'stable_sign': request})


def create_provider(req_type: RequestTypes):
    if req_type in (RequestTypes.route, RequestTypes.force_detemple):
        return RouteRequestArgumentsProvider()
    else:
        return FindTemplateRequestArgumentsProvider()


def generate(ammo_count, cursor, logger, build_request):
    result = []
    while len(result) < ammo_count:
        rows = cursor.fetchmany(10)
        if not rows:
            break

        for row in rows:
            try:
                """
                There are rows in the db having chunks=[].
                It is way more simpler to let them raise an exception during parsing than to
                filtrate out empty chunks within a SQL query, because in this case it becomes really slow.
                And in the same time this possible loss of a couple of rows does not really mater.
                """
                result.append(build_request(row))
            except Exception as e:
                logger.error(e, exc_info=True)

    if len(result) < ammo_count:
        logger.exception(
            f'Failed to build exactly {ammo_count} requests. Only {len(result)} were built.'
        )

    return result


def main():
    cmd_args = get_cmd_args()
    logger = get_logger()
    params['password'], params['sslrootcert_path'] = prepare_db_auth_data(params['vault_secret_version'])
    db_adaptor = DBAdaptor(YcDBConnectionProvider(**params))
    provider = create_provider(cmd_args.req_type)

    cursor = db_adaptor.get_cursor(provider.get_query(cmd_args.ammo_count))
    requests = generate(cmd_args.ammo_count, cursor, logger,
                        provider.get_request_builder())

    ammo = ''
    for req in requests:
        ammo_fmt_converter = AmmoFormatConverter(cmd_args.case_tag,
                                                 cmd_args.url)
        arguments_builder = provider.get_http_request_arguments_builder()
        request_arguments = arguments_builder(req)
        ammo += ammo_fmt_converter.convert(request_arguments)
    write_file(cmd_args.out_path, ammo)


if __name__ == '__main__':
    main()
