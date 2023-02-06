# -*- coding: utf-8 -*-

from mapreduce.yt.python.yt_stuff import YtStuff, YtConfig

from ytqueue import YTQueue, MultiQueue, ytq_make_attributes


YT_SERVER = None


def setup_module(module):
    module.YT_SERVER = YtStuff(YtConfig(wait_tablet_cell_initialization=True))
    module.YT_SERVER.start_local_yt()


def teardown_module(module):
    if module.YT_SERVER:
        module.YT_SERVER.stop_local_yt()


def setup_function(f):
    global YT_SERVER
    yt = YT_SERVER.get_yt_client()

    for table in yt.list("//home"):
        yt.remove("//home/{table}".format(table=table), recursive=True, force=True)


__queue_schema = [
    # dict(name='$timestamp', type='uint64'),
    dict(name='item', type='string')]


def test_yt_put_n_get():
    global YT_SERVER
    yt = YT_SERVER.get_yt_client()

    table_name = '//home/queue_put_n_get'
    ytq = YTQueue(YT_SERVER.get_server(), table_name,
                  ytq_make_attributes(__queue_schema))
    ytq.put({'item': 'First item in the queue'})
    yt.freeze_table(table_name, sync=True)
    node = ytq.get()
    assert(node['item'] == 'First item in the queue')
    assert(node['$row_index'] == 0)


def mkq(table_path, num_shards=1, num_puts=1, num_rows=100):
    global YT_SERVER
    yt = YT_SERVER.get_yt_client()
    one_day_ms = 24 * 60 * 60 * 1000
    two_days = 2 * one_day_ms
    ytq = YTQueue(YT_SERVER.get_server(), table_path,
                  ytq_make_attributes(__queue_schema, ttl_milliseconds=two_days, auto_compaction_period_milliseconds=one_day_ms))
    if num_shards > 1:
        ytq.reshard(num_shards)
    all_rows = []
    for i in range(num_puts):
        rows = [{'item': str(i)} for i in range(num_rows*i, num_rows*(i+1))]
        ytq.put_multiple(rows)
        all_rows.extend(rows)
    yt.freeze_table(table_path, sync=True)
    return (yt, ytq, all_rows)


def test_ytq_put_multiple():
    (yt, ytq, _) = mkq('//home/queue_put_multi')
    node1 = ytq.get()
    node2 = ytq.get()
    assert(node1['item'] == '0')
    assert(node2['item'] == '1')


def test_ytq_get_multiple():
    (yt, ytq, expected_rows) = mkq('//home/queue_get_multi')
    rows_read = ytq.get_multiple(len(expected_rows))
    actual = [{'item': r.get('item')} for r in rows_read]
    assert(len(rows_read) == len(expected_rows))
    assert(actual == expected_rows)


def test_ytq_seek():
    (yt, ytq, _) = mkq('//home/queue_ytq_seek')
    ytq.seek(0, 10)
    node = ytq.get()
    assert(node['item'] == '10')
    assert(node['$row_index'] == 10)


def test_python_trim_rows():
    table_path = '//home/queue_trim'
    (yt, ytq, _) = mkq(table_path)
    yt.unfreeze_table(table_path, sync=True)
    yt.trim_rows(table_path, 0, 5)
    yt.freeze_table(table_path, sync=True)
    node = ytq.get()
    assert(node['item'] == '5')
    assert(node['$row_index'] == 5)
    yt.unfreeze_table(table_path, sync=True)
    yt.trim_rows(table_path, 0, 6)
    yt.freeze_table(table_path, sync=True)
    node = ytq.get()
    assert(node['item'] == '6')
    assert(node['$row_index'] == 6)


def test_attributes():
    table_path = '//home/queue_ytq_seek'
    (yt, ytq, _) = mkq(table_path)
    min_ttl = yt.get(table_path + '/@min_data_ttl')
    max_ttl = yt.get(table_path + '/@max_data_ttl')
    auto_compaction_period = yt.get(table_path + '/@auto_compaction_period')
    default_ttl = 2 * 24 * 60 * 60 * 1000
    default_auto_compaction_period = 24 * 60 * 60 * 1000
    assert(min_ttl == 0)
    assert(max_ttl == default_ttl)
    assert(auto_compaction_period == default_auto_compaction_period)


def test_multi_tablet():
    table_path = '//home/queue_multitabs'
    global YT_SERVER
    yt = YT_SERVER.get_yt_client()
    ytq = YTQueue(YT_SERVER.get_server(), table_path,
                  ytq_make_attributes(__queue_schema))
    num_shards = 3
    num_rows = 100
    num_puts = 10
    ytq.reshard(num_shards)
    for i in range(num_puts):
        ytq.put_multiple([{'item': str(i)} for i in range(num_rows*i, num_rows*(i+1))])
    yt.freeze_table(table_path, sync=True)
    result = []
    failover = num_puts
    while failover > 0:
        r = ytq.get_multiple(num_rows)
        if len(r) == 0:
            failover -= 1
            continue
        result.extend(r)
    assert(len(result) == 10 * num_rows)


def test_ytq_attributes():
    table_path = '//home/queue_check_options'
    global YT_SERVER
    yt = YT_SERVER.get_yt_client()
    attrs = ytq_make_attributes(__queue_schema, ttl_milliseconds=100000, auto_compaction_period_milliseconds=100500)
    attrs.update({'my_user_attr': 'my_value'})
    YTQueue(YT_SERVER.get_server(), table_path, attrs)
    yt.freeze_table(table_path, sync=True)
    attrs_read = yt.get(table_path + '/@')
    assert(bool(attrs_read['dynamic']) is True)
    assert(attrs_read['my_user_attr'] == 'my_value')
    assert(attrs_read['max_data_ttl'] == 100000)
    assert(attrs_read['auto_compaction_period'] == 100500)


def test_get_offsets():
    table_path = '//home/queue_ytq_get_offsets'
    (yt, ytq, expected_rows) = mkq(table_path)
    assert(ytq.get_offsets() == [0])
    ytq.get_multiple(10)
    assert(ytq.get_offsets() == [10])


def test_ytq_set_offsets():
    table_path = '//home/queue_ytq_get_offsets'
    (yt, ytq, expected_rows) = mkq(table_path, 4, 8, 10)
    ytq.set_offsets([2, 2, 2, 2])
    assert([2, 2, 2, 2] == ytq.get_offsets())
    for i in range(4):
        node = ytq.get()
        if node is not None:
            break
    assert(node['$row_index'] % 2 == 0)


def test_ytq_save_offsets():
    table_path = '//home/queue_ytq_save_offsets'
    (yt, ytq, expected_rows) = mkq(table_path, 4)
    for i in range(4):
        ytq.get_multiple(10)
    ytq.save_offsets('mykey')
    a = yt.get(table_path + '/@offsets.mykey')
    offsets = [int(o) for o in a.split(',')]
    assert(sorted(offsets) == [0, 0, 0, 10])


def test_ytq_rewind():
    table_path = '//home/queue_ytq_rewind'
    (yt, ytq, expected_rows) = mkq(table_path)
    r0 = ytq.get()
    r1 = ytq.get()
    ytq.rewind()
    assert(ytq.get_offsets() == [0])
    r2 = ytq.get()
    assert(r0 is not None)
    assert(r0 != r1)
    assert(r0 == r2)


def test_multi_queue():
    tables = ['//home/multi_queue.100',
              '//home/multi_queue.040',
              '//home/multi_queue.050']
    queues = [mkq(t, 1, 10, 100) for t in tables]
    tables.append('//home/multi_queue.001')
    queues.append(mkq(tables[-1], 1, 1, 1))

    for (t, q) in zip(tables, queues):
        q[0].unfreeze_table(t, sync=True)

    mq = MultiQueue(YT_SERVER.get_server(), "//home/multi_queue")
    failover = 10 * 100 * 3
    result = []
    while True:
        if failover <= 0:
            raise Exception("Retry count exceeded")
        r = mq.get_multiple(100)
        result.extend(r)
        failover -= 1
        if len(result) >= 10*100*3:
            break
    assert(len(result) == 3001)
    assert(result[0]['item'] == '0')
    assert(result[1]['item'] == '0')
