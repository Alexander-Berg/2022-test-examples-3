from extsearch.images.library.testlib.base_process import BaseProcessDescription


class IndexProcessDescription(BaseProcessDescription):
    suffix_dump_modes = [
        ('metadoc', '$dumper -t metadoc'),
        ('erf', '$dumper -t erf'),
        ('i2tv7.embeddings', '$dumper -t embeddings_knn'),
        ('t2t.embeddings', '$dumper -t embeddings_knn'),
        ('url', '$dumper -t imageurl'),
        ('links.key', '$dumper -t linkdb_ranking_entry'),
        ('inputdoc.links', '$dumper -t inputdoc'),
        ('inputdoc.images.url', '$dumper -s -t inputdoc'),
        ('inputdoc.images', '$dumper -s -t inputdoc'),
        ('inputdoc.ann', '$dumper -s -t inputdoc'),
        ('inputdoc.extdata', '$dumper -s -t inputdoc'),
        ('delta.thumbdb', '$dumper -t imagedb'),
        ('/images.0', '$dumper -t imagedb'),
        ('digest3.0', '$dumper -t imagedb'),
        ('portion', '$dumper -t index_portion'),
        ('indexpanther', '$dumper -t index_panther'),
        ('indexann', '$dumper -t index'),
        ('indexannfactors', '$dumper -t index'),
        ('indexarc', '$dumper -t index'),
        ('indexcounts', '$dumper -t index_panther'),
        ('index', '$dumper -t index'),
        ('selectionrank', '$dumper -t srdocuments'),
        ('input:images.rankingentry', '$dumper -t planner_ranking_entry_list'),
        ('ranked.image.page', '$dumper -t planner_ranking_entry'),
        ('unknown', '$dumper -t planner_ranking_factors'),  # WTF?
        ('unknown', '$dumper -t planner_ranking_identity'),  # Duplicate?
        ('images.url.key', '$dumper -t hex'),
        ('images.key', '$dumper -t hex'),
        ('images.documents.key', '$dumper -t hex'),
        ('imageurl2document', '$dumper -t hex'),
        ('selecteddocuments', '$dumper -t hex'),
        ('result:userdoc', '$dumper -t hex'),
        ('userdoc.shard.table', '$dumper -t shardtable'),
        ('index.shard.table', '$dumper -t shardtable'),
        ('index.panther.shard.table', '$dumper -t shardtable'),
        ('archive.shard.table', '$dumper -t shardtable'),
        ('metadoc.shard.table', '$dumper -t shardtable'),
        ('cbir.index.shard.table', '$dumper -t shardtable'),
        ('cbir.shard.table', '$dumper -t shardtable'),
        ('result:queryinfo', '$dumper -t hex'),
        ('parse.mapped', '$dumper -t linkdb_exports'),
        ('parse.export', '$dumper -t kiwi_record'),
        ('links.0', '$dumper -t linkdb'),
        ('links.1', '$dumper -t linkdb'),
        ('links.2', '$dumper -t linkdb'),
        ('links.3', '$dumper -t linkdb'),
        ('rankingentry', '$dumper -t linkdb_ranking_entry'),
        ('rankingentry.delta', '$dumper -t linkdb_ranking_entry'),
        ('rankingentry.pages', '$dumper -t linkdb_ranking_entry'),
        ('rankingentry.aggregated', '$dumper -t linkdb_ranking_entry_list'),
        ('full_table_list', 'sort -u'),
        ('cbir/shard', '$dumper -s -t proto NImages.NIndex.NYt.TCbirShardDataPB'),
        ('cbir/shard.pics', '$dumper -t hex'),
        ('cbir/shard.pics.ranged', '$dumper -t cbir_shard'),
        ('cbir_index/cbir', '$dumper -t index'),
        ('cbir_index/shard', '$dumper -t index'),
        ('url.0', '$dumper -t imagedb_url'),
        ('url.1', '$dumper -t imagedb_url'),
        ('url.2', '$dumper -t imagedb_url'),
        ('url.delta', '$dumper -t imagedb_url'),
        ('url.deleted', '$dumper -t imagedb_url'),
        ('url.redirects', '$dumper -t imagedb_url'),
        ('url.sources', '$dumper -t hex'),
        ('url.delta', '$dumper -t imagedb_url'),
        ('result/usertrie', '$dumper -t usertrie'),
        ('20161130-025323', '$dumper -t export2ukrop'),
        ('ack.20221212-121212', '$dumper -t export_pages_2_kwyt'),
        ('urltracer/inputdoc/inputdoc.pages.0', '$dumper -t traceroute_status'),
        ('urltracer/inputdoc/inputdoc.images.0', '$dumper -t traceroute_status'),
        ('urltracer/input/images.rankingentry', '$dumper -t traceroute_status'),
        ('urltracer/input/images.rankingentry.url', '$dumper -t traceroute_status'),
        ('urltracer/input/links.aggregated.images.0', '$dumper -t traceroute_status'),
        ('urltracer/input/links.aggregated.pages.0', '$dumper -t traceroute_status'),
        ('urltracer/metadoc/metadoc.images.0', '$dumper -t traceroute_status'),
        ('urltracer/metadoc/metadoc.pages.0', '$dumper -t traceroute_status'),
        ('urltracer/remap.images.0', '$dumper -t traceroute_status'),
        ('urltracer/remap.pages.0', '$dumper -t traceroute_status'),
        ('urltracer/inputdoc/inputdoc.pages', '$dumper -t traceroute_status'),
        ('urltracer/inputdoc/inputdoc.images', '$dumper -t traceroute_status'),
        ('urltracer/metadoc/metadoc.images', '$dumper -t traceroute_status'),
        ('urltracer/metadoc/metadoc.pages', '$dumper -t traceroute_status'),
        ('urltracer/remap.images', '$dumper -t traceroute_status'),
        ('urltracer/remap.pages', '$dumper -t traceroute_status'),
        ('urltracer/result.images', '$dumper -t traceroute_status'),
        ('urltracer/result.pages', '$dumper -t traceroute_status')
    ]
    regex_dump_modes = [
        ('/statistics[^/]*$', '$dumper -t text'),  # any statistics tables are just a plain text
        ('/statistics/', '$dumper -t text'),
        ('urldb/[^/]+/[0-9]/(images|pages|redirects|other)(\.newkey)?$', '$dumper -t urldb'),
        ('urldb/[^/]+/[0-9]/delta\.[^/]+$', '$dumper -s -t urldb'),
        ('hostdb/[^/]+/(status|status\.delta)$', '$dumper -s -t hostdb'),
        ('hostdb/[^/]+/status\.unavailable$', '$dumper -s -t text'),
        ('streamdb/[^/]+/[^/]+$', '$dumper -s -t hex'),
        ('cbir_index/sim.[^/]+', '$dumper -t index'),
    ]
