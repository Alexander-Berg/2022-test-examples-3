from parallel_offline import parse_parallel_daas as ppd
from parallel_offline import join_daas as jd
from parallel_offline import join_toloka as jt
from parallel_offline import join_testids as jtt
from parallel_offline import offline_basket as ob
from parallel_offline import calc_offline_metrics as com
from parallel_offline import mock_daas
from parallel_offline import mock_toloka


def test_integral(serps):
    """
    :param serps: path to "backend serps", a result from daas parallel_extract_wizards decoder
    """
    wd_after_serps_pp = ppd.read_extract_wizards_decoder(serps)
    daas_urls_pp = ppd.add_daas_info(wd_after_serps_pp)

    daas = mock_daas.DaasMock()
    daas_answer_pp = daas.process_urls(daas_urls_pp)
    daas_answer_pp.columns = jd.DAAS_FORMAT_COLUMNS

    wd_after_daas_pp = jd.merge_serps_with_daas(wd_after_serps_pp, daas_answer_pp, "15092")

    wd_after_basket_pp = ob.BasketIo.basket_collection_to_df(
        ob.get_basket_collection(wd_after_daas_pp, 'backend'))
    toloka_tasks_pp = jtt.filter_data_for_toloka(wd_after_basket_pp)

    toloka = mock_toloka.TolokaMock()
    toloka_answer_pp = toloka.process_dataframe(toloka_tasks_pp)

    wd_after_toloka_pp = jt.merge_relevance(wd_after_basket_pp, toloka_answer_pp, mapper='limus')
    assert not wd_after_toloka_pp.duplicated().any()

    stats_pp = com.get_df_stats(wd_after_toloka_pp,'pfound')
    stats_pp_pretty = com.pretty_print_stats(stats_pp)
    assert len(stats_pp_pretty.split('\n')) >= 19

    # test ignore toloka mode
    wd_after_toloka_pp.mapped_grade = None
    stats_pp = com.get_df_stats(wd_after_toloka_pp,'pfound')
    assert len(stats_pp) >= 19
