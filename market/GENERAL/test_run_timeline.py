from flask import render_template

from lib.ui.component.xrange_chart import XRangeChartProps


def render_test_run_timeline_page(test_run_id):
    chart_props = XRangeChartProps(
        chart_id='test-run-timeline',
        chart_title='Assignees',
        categories=[],
        series_list=[],
        series_endpoint='/api/v1/test-room/timeline?test-run-id=%s' % test_run_id
    )
    return render_template('page_test-run-timeline.html',
                           test_run_id=test_run_id,
                           test_run_chart_props=chart_props)
