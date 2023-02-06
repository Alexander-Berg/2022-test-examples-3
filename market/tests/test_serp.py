from parallel_offline import parse_serps as ps
from parallel_offline import join_daas as jd
from parallel_offline import join_toloka as jt
from parallel_offline import join_testids as jtt
from parallel_offline import offline_basket as ob
from parallel_offline import calc_offline_metrics as com
from parallel_offline import mock_daas
from parallel_offline import mock_toloka
from parallel_offline import calc_offline_dashboard as cod


def test_integral(serps):
    """
    :param serps: path to "serps", a result from scraper operation at nirvana
    """
    testid = "15092"
    data = ps.read_json(serps)

    wd_after_serps_ss = ps.process_serps_batch(data)
    daas_urls_ss = ps.add_daas_info(wd_after_serps_ss)

    daas = mock_daas.DaasMock()
    daas_answer_ss = daas.process_urls(daas_urls_ss)
    daas_answer_ss.columns = jd.DAAS_FORMAT_COLUMNS

    wd_after_daas_ss = jd.merge_serps_with_daas(wd_after_serps_ss, daas_answer_ss, testid, mapper='limus')

    wd_after_basket_ss = ob.BasketIo.basket_collection_to_df(
        ob.get_basket_collection(wd_after_daas_ss, 'serp_all'))
    toloka_tasks_ss = jtt.filter_data_for_toloka(wd_after_basket_ss)

    toloka = mock_toloka.TolokaMock()
    toloka_answer_ss = toloka.process_dataframe(toloka_tasks_ss)

    wd_after_toloka_ss = jt.merge_relevance(wd_after_basket_ss, toloka_answer_ss)
    assert not wd_after_toloka_ss.duplicated().any()

    stats_ss = com.get_df_stats(wd_after_toloka_ss,'pfound')
    stats_ss_pretty = com.pretty_print_stats(stats_ss)
    assert len(stats_ss_pretty.split('\n')) >= 19

    # test ignore toloka mode
    wd_after_toloka_ignore = wd_after_toloka_ss.copy()
    wd_after_toloka_ignore.mapped_grade = None
    stats_ignore = com.get_df_stats(wd_after_toloka_ignore,'pfound')
    assert len(stats_ignore) >= 19

    com.get_adg_stats(stats_ss, testid)
    stats_pp = stats_ss[testid].reset_index().copy()
    statface_data = cod.format_data_to_statface(stats_pp, date="2018-01-12", check=True, dataframe=False)
    assert len(statface_data) >= 19
