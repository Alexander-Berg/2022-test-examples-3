# coding: utf-8

from time import time, localtime, strftime
import inspect
import os
import yatest.common

from market.pylibrary.mbostuff.mbomodels import read_mbo_category
from market.sailor.beam.service import SailorServer
from market.pylibrary.lite.beam import BeamHolder
from saas.protos import rtyserver_pb2
from yt.wrapper import ypath_join

from market.idx.generation.yatf.resources.books_indexer.yt_book_stuff import YTBookStuff
from market.idx.pylibrary.murmur.hash2 import hash64 as murmur_hash64

from market.proto.feedparser.deprecated.OffersData_pb2 import Offer as OfferPb
from market.proto.ir.UltraController_pb2 import EnrichedOffer as EnrichedOfferPb
from market.proto.content.mbo.ExportReportModel_pb2 import (
    ExportReportModel,
    LocalizedString,
    Picture,
    TAggregatedParam,
)
from market.proto.content.pictures_pb2 import Picture as PicturePb

from market.pylibrary.snappy_protostream import SnappyProtoWriter

from market.idx.yatf.resources.resource import FileResource
from market.idx.yatf.resources.yt_table_resource import YtTableResource
from market.idx.yatf.test_envs.base_env import BaseEnv
from market.idx.yatf.utils.genlog import genlog_table_schema

from market.pylibrary.pbufsn_utils import pbuf_lenval_from_data
from market.pylibrary.proto_utils import message_from_data


def _OFFERS_STUBS_DIR():
    return os.path.join(
        yatest.common.source_path(),
        'market', 'idx', 'offers', 'yatf',
        'resources',
        'offers_indexer',
        'stubs',
    )


def _MODELS_STUBS_DIR():
    return os.path.join(
        yatest.common.source_path(),
        'market', 'idx', 'models', 'yatf',
        'resources',
        'models_indexer',
        'stubs',
    )


def _BOOKS_STUBS_DIR():
    return os.path.join(
        yatest.common.source_path(),
        'market', 'idx', 'generation', 'yatf',
        'resources',
        'books_indexer',
        'stubs',
    )


def now_seconds():
    return int(time())


def now_days():
    return int(time())/(60*60*24)


def now_minutes():
    return int(time())/60


def message_to_dict(msg):
    result = {
        'key': msg.Document.Url,
        'deadline': msg.Document.DeadlineMinutesUTC
    }

    for prop in msg.Document.DocumentProperties:
        result[prop.Name] = prop.Value

    return result


def message_from_dict(key, data):
    msg = rtyserver_pb2.TMessage()
    msg.Document.Url = key
    msg.MessageType = rtyserver_pb2.TMessage.MODIFY_DOCUMENT
    for key, value in data.items():
        prop = msg.Document.DocumentProperties.add()
        prop.Name = key
        prop.Value = value
    return msg


def whoami():
    return inspect.getouterframes(inspect.currentframe())[1][3]


def create_genlog_table(yt_stuff, rows, table_path):
    schema = genlog_table_schema()
    print('Schema {}'.format(schema))

    table = YtTableResource(
        yt_stuff=yt_stuff,
        path=ypath_join(table_path),
        data=rows,
        attributes={
            'schema': schema
        }
    )

    if rows:
        table.create()

    return table.get_path()


class SnippetYt(object):
    class StateTables(object):
        def __init__(self, snippet_yt, rows):
            self.offers = self.create(rows, snippet_yt, 1, "OFFER")
            self.models = self.create(rows, snippet_yt, 2, "MODEL")

        @staticmethod
        def create(rows, snippet_yt, code, name):
            schema = [
                {'name': 'doc_type', 'type': 'uint32'},
                {'name': 'key', 'type': 'string'},
                {'name': 'value', 'type': 'string'},
                {'name': 'value_hash', 'type': 'uint64'},
                {'name': 'table_type', 'type': 'uint32'},
                {'name': 'diff_priority', 'type': 'uint32'},
                {'name': 'deleted', 'type': 'boolean'},
                {'name': 'last_update_seconds', 'type': 'uint64'}
            ]

            rows = [xx for xx in rows if xx["doc_type"] == code]
            if len(rows) == 0:
                return None
            table_offers = YtTableResource(
                yt_stuff=snippet_yt.server,
                path=ypath_join(snippet_yt.yt_test_dir, 'input/state_{0}'.format(name)),
                data=rows,
                attributes={'schema': schema}
            )
            table_offers.create()
            return table_offers.get_path()

    def __init__(self, server, yt_test_dir):
        self.server = server
        self.yt_client = self.server.get_yt_client()
        self.yt_test_dir = yt_test_dir
        self.input_models_dir = ypath_join(self.yt_test_dir, 'input/models')

    def create_offer_table(self, rows, table_path):
        schema = [
            {'name': 'feed_id', 'type': 'uint64'},
            {'name': 'session_id', 'type': 'uint64'},
            {'name': 'offer_id', 'type': 'string'},
            {'name': 'offer', 'type': 'string'},
            {'name': 'recs', 'type': 'string'},
            {'name': 'promo', 'type': 'string'},
            {'name': 'uc', 'type': 'string'},
            {'name': 'pic', 'type': 'string'},
            {'name': 'couple_id', 'type': 'uint64'},
            {'name': 'diff_type', 'type': 'string'},
        ]
        table = YtTableResource(
            yt_stuff=self.server,
            path=table_path,
            data=rows,
            attributes={'schema': schema}
        )

        table.create()
        return table.get_path()

    def create_offer_tables(self, rows):
        table_num = 2
        table_data = [rows[i::table_num] for i in range(table_num)]
        dir_path = ypath_join(self.yt_test_dir, 'input/offers')
        for i in range(table_num):
            self.create_offer_table(table_data[i], ypath_join(dir_path, str(i)))

        return dir_path

    def create_genlog_tables(self, rows):
        table_num = 2
        table_data = [rows[i::table_num] for i in range(table_num)]
        dir_path = ypath_join(self.yt_test_dir, 'input/genlog')
        for i in range(table_num):
            create_genlog_table(self.server, table_data[i], ypath_join(dir_path, str(i)))
        if rows:
            return dir_path
        return None

    def create_state_table(self, rows):
        return SnippetYt.StateTables(self, rows)

    def create_models_table(self, models):
        schema = [
            {'name': 'category_id', 'type': 'uint64', 'sort_order': 'ascending', 'required': True},
            {'name': 'model_id', 'type': 'uint64'},
            {'name': 'data', 'type': 'string'},
        ]

        def row_generator():
            for model in models:
                yield {
                    'model_id': model.id,
                    'category_id': model.category_id,
                    'data': model.SerializeToString()
                }

        table = YtTableResource(
            yt_stuff=self.server,
            path=ypath_join(self.input_models_dir, 'models/models'),
            data=row_generator(),
            attributes={'schema': schema}
        )

        table.create()
        return table

    def create_parameters_table(self, parameters_filepath):
        schema = [
            {'name': 'hid', 'type': 'uint64', 'sort_order': 'ascending', 'required': True},
            {'name': 'data', 'type': 'string'},
        ]

        params = read_mbo_category(parameters_filepath)
        row = {
            'hid': params.hid,
            'data': params.proto.SerializeToString()
        }

        table = YtTableResource(
            yt_stuff=self.server,
            path=ypath_join(self.input_models_dir, 'models/parameters'),
            data=[row],
            attributes={'schema': schema}
        )

        table.create()
        return table

    def create_book_stuff_table(self, books):
        dir_path = ypath_join(self.yt_test_dir, 'bookstuff')

        book_stuff_resourse = YTBookStuff.from_list(
            self.server, self.server.get_yt_client(),
            dir_path,
            books
        )

        book_stuff_resourse.init(self)
        return book_stuff_resourse

    def create_vcluster_descriptions_table(self, vcluster_descriptions):
        schema = [
            {'name': 'cluster_id', 'type': 'int64'},
            {'name': 'description', 'type': 'string'},
            {'name': 'hid', 'type': 'uint64'},
        ]

        table = YtTableResource(
            yt_stuff=self.server,
            path=ypath_join(self.yt_test_dir, 'vcluster_pictures'),
            data=vcluster_descriptions,
            attributes={'schema': schema}
        )

        table.create()
        return table

    def exists(self, path):
        return self.server.get_yt_client().exists(path)


class SnippetDiffBuilderTestEnv(BaseEnv):
    UNCHAGED_TTL = 100500
    DELETED_TTL = 100500
    DEFAULT_TTL = UNCHAGED_TTL + 7

    _MATCHERS = [
    ]

    def __init__(
            self,
            testname,
            server,
            offers,
            models,
            state,
            quota=None,
            deleted_ttl=None,
            unchanged_ttl=None,
            book_stuff=None,
            vcluster_descriptions=None,
            do_not_delete_docs=False,
            additional_offers=None,
            use_pokupki_domain=False,
            genlogs=None,
            glue_config_path=None,
            **resources
    ):
        if genlogs is None:
            genlogs = []
        self._STUBS = {
            name: FileResource(os.path.join(_OFFERS_STUBS_DIR(), filename))
            for name, filename in {
                'currency_rates': 'currency_rates.xml',
                'gl_mbo_pbuf_sn': 'gl_mbo.pbuf.sn',
                'ungrouping_model_params_gz': 'ungrouping_model_params.gz',
                'ungrouping_models_gz': 'ungrouping_models.gz'
            }.items()
        }
        self._STUBS.update({
            name: FileResource(os.path.join(_MODELS_STUBS_DIR(), filename))
            for name, filename in {
                'tovar_categories': 'tovar-tree.pb',
                'parameters': 'parameters_90592.pb',
            }.items()
        })
        self._STUBS.update({
            name: FileResource(os.path.join(_BOOKS_STUBS_DIR(), filename))
            for name, filename in {
                'thumbs_config': 'picrobot_thumbs.meta',
            }.items()
        })
        if not additional_offers:
            additional_offers = []

        super(SnippetDiffBuilderTestEnv, self).__init__(**resources)
        stime = strftime("%Y_%m_%d_%H_%M_%S", localtime())
        self.yt_test_dir = ypath_join('//home/test/snippet_diff_builder', testname, stime)
        self.yt_client = server.get_yt_client()
        self.yt = SnippetYt(server, self.yt_test_dir)
        self.input_offer_dir = self.yt.create_offer_tables(offers)
        self.input_offer_table = self.yt.create_offer_table(
            additional_offers,
            ypath_join(self.yt_test_dir, 'input/deleted_offers')
        )
        self.input_genlog_dir = self.yt.create_genlog_tables(genlogs)
        self.input_state_tables = self.yt.create_state_table(state)
        self.input_models_table = self.yt.create_models_table(models)
        self.output_diff_table_path = ypath_join(self.yt_test_dir, 'output/offers')
        self.output_reverse_diff_table_path = ypath_join(self.yt_test_dir, 'output/reverse_diff')
        self.output_state_table_path = ypath_join(self.yt_test_dir, 'output/state')
        self.quota = quota or 100500
        self.exec_result = None
        self.deleted_ttl = deleted_ttl or self.UNCHAGED_TTL
        self.unchanged_ttl = unchanged_ttl or self.DELETED_TTL
        self.book_stuff = book_stuff
        if self.book_stuff:
            self.input_book_stuff_table = self.yt.create_book_stuff_table(self.book_stuff)

        self.vcluster_descriptions = vcluster_descriptions
        if self.vcluster_descriptions:
            self.vcluster_descriptions_table = self.yt.create_vcluster_descriptions_table(
                self.vcluster_descriptions
            )
        self.models = models
        self.do_not_delete_docs = do_not_delete_docs
        self.testname = testname
        self.use_pokupki_domain = use_pokupki_domain
        self.glue_config_path = glue_config_path

    def generate_group_ranges_files(self, models, dst_dir):
        # Это очень плохая реализация мержа параметров из модификации для использования в групповой модели.
        # Оригинальынй мерж гораздо сложнее.
        # См. https://a.yandex-team.ru/arc/trunk/arcadia/market/indexer/models/mbo-info-extractor/group_parameters_aggregator.cpp?rev=5030297#L227

        params = {}  # params[hid][parent_model_id][param_id] = param

        for model in models:
            if not model.parent_id:  # not modification
                continue

            hid = model.category_id

            for param_value in model.parameter_values:
                params.setdefault(hid, {})
                params[hid].setdefault(model.parent_id, {})
                params[hid][model.parent_id][param_value.param_id] = param_value

        for hid, ranges in params.items():
            dst_filepath = os.path.join(dst_dir, 'group_{}.pbuf.sn'.format(hid))
            with open(dst_filepath, 'wb') as fn:
                with SnappyProtoWriter(fn, 'MBEM') as snappy_writer:
                    for model_id, model_params in ranges.items():
                        model = ExportReportModel()
                        model.id = model_id

                        for param in model_params.values():
                            aggregated_param = TAggregatedParam()
                            aggregated_param.param_id = param.param_id
                            aggregated_param.type = param.type_id

                            if param.bool_value:
                                aggregated_param.bool_values.extend([param.bool_value])

                            if param.numeric_value:
                                aggregated_param.numeric_enum_values.extend([param.numeric_value])

                            aggregated_param.string_values.extend(param.str_value)

                            model.group_range_params.extend([aggregated_param])

                        snappy_writer.write(model)

    @property
    def description(self):
        return 'snippet-diff-builder'

    @property
    def output_diff_table(self):
        return [decode_value(row) for row in self.outputs.get('diff').data]

    @property
    def output_reverse_diff_table(self):
        return [decode_value(row) for row in self.outputs.get('reverse_diff').data]

    @property
    def output_state_table(self):
        return [decode_value(row) for row in self.outputs.get('state').data]

    def execute(self):
        group_ranges_dst_dir = os.path.join(
            yatest.common.test_output_path('group_ranges'),
            self.testname
        )
        os.makedirs(group_ranges_dst_dir)
        self.generate_group_ranges_files(self.models, group_ranges_dst_dir)

        self.yt.create_parameters_table(self.resources['parameters'].path)

        def configure_beam(sailor_svc):
            sailor_svc.use_yt_wrapper(self.yt.server)
            sailor_svc.clear_yt()
            sailor_svc. \
                configure_model_input(tovar_tree_path=self.resources['tovar_categories'].path,
                                      thumbs_path=self.resources['thumbs_config'].path,
                                      input_models_dir=self.yt.input_models_dir,
                                      vcluster_descriptions=self.vcluster_descriptions_table.get_path() if self.vcluster_descriptions else None,
                                      book_stuff=self.input_book_stuff_table.table_path if self.book_stuff else None,
                                      modification_ranges_dir=group_ranges_dst_dir)
            sailor_svc.configure_order_prepare(
                deleted_ttl=self.deleted_ttl, unchanged_ttl=self.unchanged_ttl, dont_delete=self.do_not_delete_docs)

            if self.quota:
                sailor_svc.configure_sender(self.quota)

        sailor_holder = BeamHolder(SailorServer, configure=configure_beam, cleanup=True)
        sailor_beam = sailor_holder.beam

        model_paths = sailor_holder.beam.yt_paths("MODEL")
        offer_paths = sailor_holder.beam.yt_paths("OFFER")
        yt_client = self.yt.server.get_yt_client()

        sailor_beam.clear_yt()
        sailor_holder.start()
        try:
            if self.input_state_tables.models:
                yt_client.copy(self.input_state_tables.models, model_paths.state, recursive=True, force=True)

            if self.input_state_tables.offers:
                yt_client.copy(self.input_state_tables.offers, offer_paths.state, recursive=True, force=True)

            self._run_offers_converter("localhost:{0}".format(sailor_beam.port), "OFFER")
            sailor_beam.do_iteration("MODEL", "INPUT")
            sailor_beam.do_iteration("MODEL")
            sailor_beam.do_iteration("OFFER")
        finally:
            sailor_holder.stop()

        yt_client.create("map_node", ypath_join(self.yt_test_dir, 'output'), True, True)

        model_paths = sailor_holder.beam.yt_paths("MODEL")
        offer_paths = sailor_holder.beam.yt_paths("OFFER")

        yt_client.run_merge([offer_paths.diff, model_paths.diff], self.output_diff_table_path)
        yt_client.run_merge([offer_paths.state, model_paths.state], self.output_state_table_path)
        if model_paths.reverse_diff and offer_paths.reverse_diff:
            yt_client.run_merge([offer_paths.reverse_diff, model_paths.reverse_diff],
                                self.output_reverse_diff_table_path, mode='ordered')

        self.outputs.update({
            'diff': YtTableResource(self.yt.server, self.output_diff_table_path, load=True),
            'reverse_diff': YtTableResource(self.yt.server, self.output_reverse_diff_table_path, load=True),
            'state': YtTableResource(self.yt.server, self.output_state_table_path, load=True)
        })
        self.verify()

    def _run_offers_converter(self, sailor_url, collection, ignore_blue=False):
        args = [
            yatest.common.binary_path(
                os.path.join('market', 'sailor', 'offers_converter', 'snippets_offers_converter')),
            '--yt-proxy', self.yt.server.get_server(),
            '--input-offer-table-dir', self.input_offer_dir,
            '--input-offer-table', self.input_offer_table,
            '--currency-exchange-path', self.resources['currency_rates'].path,
            '--sailor-collection-type', collection,
            '--ping-sailor-url', sailor_url,
            '--gl-mbo-path', self.resources['gl_mbo_pbuf_sn'].path,
            '--ungrouping-model-params-path', self.resources['ungrouping_model_params_gz'].path,
            '--ungrouping-models-path', self.resources['ungrouping_models_gz'].path
        ]

        if self.input_genlog_dir:
            args += ['--input-genlog-table-dir', self.input_genlog_dir]

        if ignore_blue:
            args.append('--ignore-blue')

        if self.use_pokupki_domain:
            args.append('--use-pokupki-domain')

        if self.glue_config_path:
            args.append('--glue-config-path')
            args.append(self.glue_config_path)

        return self.try_execute_under_gdb(
            args,
            cwd=self.output_dir
        )

    def exists(self, path):
        return self.yt.exists(path)

    @staticmethod
    def _get_row_names_by_schema(schema):
        schema_names = []
        for sc in schema:
            schema_names.append(sc['name'])
        return schema_names


class OfferRow(dict):
    def __init__(
        self,
        feed_id,
        offer_id,
        url=None,
        title=None,
        title_no_vendor=None,
        comment=None,
        offer_comment=None,
        shop_name=None,
        shop_category_path=None,
        vat=None,
        picUrls=None,
        weight=None,
        length=None,
        height=None,
        width=None,
        cpa=None,
        enable_auto_discount=None,
        cargo_types=None,
        seller_warranty=None,
        raw_params=None,
        reference_oldprice=None,
        recommended_price=None,
        price_history=None,
        oldprice_expression=None,
        price_limit=None,
        pic=None,
        recs=None,
        promo=None,
        vendor=None,
        raw_vendor=None,
        supplier_description=None,
        manufacturer_country_ids=None,
        is_blue=False,
        formalized_params=None,  # a list of FormalizedParamPosition
        category_id=90401,
        is_fake_msku_offer=False,
        mbo_model=None,
        model_id=0,
        matched_id=0,
        cluster_id=0,
        ware_md5=None,
        top_queries_offer=None,
        top_queries_all=None,
        min_quantity=1,
        step_quantity=1,
        credit_templates=None,
        contex_info=None,
        sales_notes=None,
        description=None,
        vendor_code=None,
    ):
        if formalized_params is None:
            formalized_params = []

        offer = {
            'URL': url or 'naberu.ru/doxyya.ya',
            'title': title or 'lucky good',
            'Comment': comment or 'shop comment',
            'yx_shop_name': shop_name or 'ozon.ru',
            'yx_shop_category_path': shop_category_path or 'smartphones/iphones',
            'vat': vat or 2,
            'feed_group_id_hash': 'abcdefg',
            'ware_md5': ware_md5 or 'xyzabc',
            'picURLS': picUrls or 'some-pic-urls',
            'weight': weight or 1.0,
            'length': length or 2.0,
            'height': height or 3.0,
            'width': width or 4.0,
            'cpa': cpa or 4,
            'enable_auto_discounts': enable_auto_discount or True,
            'cargo_types': cargo_types or [456, 789],
            'is_blue_offer': is_blue,
            'seller_warranty': seller_warranty or 'nicht',
            'binary_reference_oldprice': {
                'price': reference_oldprice or 123
            },
            'price_history': price_history or {
                'is_valid': True,
                'price_expression': 'RUR 12000000',
                'date_yyyymmdd': 20180831,
                'min_price_expression': 'RUR 11000000',
                'min_price_date_yyyymmdd': 20180731
            },
            'recommended_price': {
                'price': recommended_price or 555
            },
            'oldprice_expression': oldprice_expression or '3333.000000 1 0 RUR RUR',
            'supplier_description': supplier_description or 'best offer ever',
            'manufacturer_country_ids': manufacturer_country_ids or '116',
            'top_queries_offer': top_queries_offer,
            'top_queries_all': top_queries_all,
            'is_fake_msku_offer': is_fake_msku_offer,
            'binary_price_limit': {
                'price': price_limit or 456
            },
            'MinQuantity': min_quantity or 1,
            'StepQuantity': step_quantity or 1,
            'sales_notes': sales_notes or 'sales_notes',
            'description': description or 'description',
            'vendor_code': vendor_code or 'CE400X',
        }

        if title_no_vendor is not None:
            offer['title_no_vendor'] = title_no_vendor

        if vendor is not None:
            offer['vendor'] = vendor

        if raw_vendor is not None:
            offer['raw_vendor'] = raw_vendor

        if mbo_model is not None:
            offer['mbo_model'] = mbo_model

        if credit_templates is not None:
            offer['credit_templates'] = credit_templates

        if contex_info is not None:
            offer['genlog'] = {
                'contex_info': contex_info,
            }

        super(OfferRow, self).__init__({
            'feed_id': feed_id,
            'offer_id': str(offer_id),
            'offer': message_from_data(offer, OfferPb()).SerializeToString(),
            'pic': pbuf_lenval_from_data(pic or [
                {'group_id': 5, 'dups_id': 7},
                {'group_id': 6, 'dups_id': 8}
            ], PicturePb),
            'recs': recs or 'abcdefghabcdefgh',  # length MUST be multiple of 16 bytes
            'promo': promo or None,
            'uc': EnrichedOfferPb(
                params=formalized_params,
                category_id=category_id,
                model_id=model_id,
                matched_id=matched_id,
                cluster_id=cluster_id,
            ).SerializeToString(),
        })


def ModelRow(title=None, category_id=90592, current_type='GURU', published_on_market=True, *args, **kwargs):
    # inhereting from ExportReportModel doesn't work: got TypeError
    model = ExportReportModel(
        *args,
        category_id=category_id,
        current_type=current_type,
        published_on_market=published_on_market,
        **kwargs
    )

    if title:
        localized_string = LocalizedString(isoCode='ru', value=title)
        model.titles.extend([localized_string])

    picture1 = Picture(
        xslName='XL-Picture',
        url='http://market.yandex.ru/model.jpg',
        width=100,
        height=100
    )

    model.pictures.extend([picture1])

    return model


def decode_value(fields):
    if isinstance(fields['value'], dict):
        return fields
    msg = rtyserver_pb2.TMessage()
    msg.ParseFromString(fields['value'])
    fields['value'] = message_to_dict(msg)
    fields['message_type'] = msg.MessageType
    return fields


class DocumentStateRow(dict):
    @staticmethod
    def calc_hash(props):
        print('HASH PROPS: {}'.format(props))
        hash = murmur_hash64('\n'.join([' '.join(kv) for kv in sorted(props.items())]))
        print('HASH: {}'.format(hash))
        return hash

    def __init__(
            self,
            last_update_seconds=None,
            add_field=None,
            del_field=None,
            props=None,
            deleted=False,
    ):
        if last_update_seconds is None:
            last_update_seconds = now_seconds()

        super(DocumentStateRow, self).__init__()
        self['deleted'] = deleted
        self['last_update_seconds'] = last_update_seconds
        self['diff_priority'] = None

        if props is None:
            props = {}

        if add_field:
            props[add_field] = 'hello'

        if del_field:
            del props[del_field]

        self['value'] = message_from_dict(self['key'], props).SerializeToString()
        self['value_hash'] = self.calc_hash(props)


class OfferStateRow(DocumentStateRow):
    def __init__(
            self,
            feed_id,
            offer_id,
            title='lucky good',
            *args,
            **kwargs
    ):
        self['doc_type'] = 1
        self['table_type'] = 0
        self['key'] = '{}-{}'.format(feed_id, offer_id)
        props = {
            'LANG': 'ru',
            'reference_old_price': '0.0000123',
            'rec': 'YWJjZGVmZ2hhYmNkZWZnaA',
            'seller_warranty': 'nicht',
            'cargo_types': '456/789',
            'history_price': '1.2',
            '_Url': 'naberu.ru/doxyya.ya',
            '_Title': title,
            'picture_flags': '7|8',
            'offer_url_hash': '15581832340287854437',
            'PicturesProtoBase64': 'KAVgBw,,|KAZgCA,,',
            'vat': '2',
            'weight': '1',
            'length': '2',
            'height': '3',
            'width': '4',
            'unverified_old_price': '3333',
            'recommended_price': '0.0000555',
            'enable_auto_discounts': '1',
            'supplier_description': 'best offer ever',
            'manufacturer_country_ids': '116',
            'price_limit': '0.0000456',
            'min_quantity': '1',
            'step_quantity': '1',
            'shop_category_path': 'smartphones/iphones',
            'sales_notes': 'sales_notes',
            'description': 'description',
            'vendor_code': 'CE400X',
        }

        if kwargs.get('props', None) is not None:
            props.update(kwargs['props'])
            del kwargs['props']

        super(OfferStateRow, self).__init__(*args, props=props, **kwargs)


class ModelStateRow(DocumentStateRow):
    def __init__(
            self,
            model_id,
            title='',
            *args, **kwargs
    ):

        self['doc_type'] = 2
        self['table_type'] = 0
        self['key'] = 'model-{}'.format(model_id)

        props = {
            'ModelId': str(model_id),
            'description': '',
            'ProtoPicInfo': 'CiFodHRwOi8vbWFya2V0LnlhbmRleC5ydS9tb2RlbC5qcGcQZBhk',
            'PicInfo': 'http://market.yandex.ru/model.jpg#100#100',
            '_Url': 'market.yandex.ru/product/{}'.format(model_id),
            '_Title': title,
        }

        super(ModelStateRow, self).__init__(*args, props=props, **kwargs)


class VClusterDescriptionRow(dict):
    def __init__(self, cluster_id, description, hid):
        self['cluster_id'] = cluster_id
        self['description'] = description
        self['hid'] = hid
