#!/usr/bin/env python

from __future__ import print_function
from lib import serps
from urlparse import urlparse
import argparse
import sys
import json

RESULT = {
	'ok': 0,
	'warning': 1,
	'failed': 2,
}

class LeakagePercentDiffTest:
	def __init__(self, max_percent_delta, pvalue, result):
		self.max_percent_delta = max_percent_delta
		self.pvalue = pvalue
		self.result = result

	def __call__(self, metric):
		pValue = metric['diff']['pValue']
		percent_delta = metric['diff']['percent']

		if pValue > self.pvalue and percent_delta > self.max_percent_delta:
			return self.result

		return 'ok'

class PercentDiffTest:
	def __init__(self, max_percent_delta, pvalue, result):
		self.max_percent_delta = max_percent_delta
		self.pvalue = pvalue
		self.result = result

	def __call__(self, metric):
		pValue = metric['diff']['pValue']
		percent_delta = metric['diff']['percent']

		if pValue > self.pvalue and percent_delta < self.max_percent_delta:
			return self.result

		return 'ok'

class MetricValueTest:
	def __init__(self, value_func, result):
		self.value_func = value_func
		self.result = result


	def __call__(self, metric):
		right_value = metric['leftData']['value']

		if self.value_func(right_value):
			return self.result

		return 'ok'


def run_test(serps, info):
	(name, query_set, region, tests, eval_threshold) = info
	metric = serps.get_serp_metric(query_set, region, name)
	percent_delta = metric['diff']['percent']
	pValue = metric['diff']['pValue']
	color = metric['diff']['verboseSignification']
	left_value = metric['leftData']['value']
	right_value = metric['rightData']['value']
	link = serps.get_link(query_set, region)

	result = 'ok'
	for test in tests:
		test_result = test(metric)
		if RESULT[test_result] > RESULT[result]:
			result = test_result

	beta_serp_id = urlparse(link).query.split('&')[0].split('=')[1]

	judged_metric = serps.get_serp_metric(query_set, region, 'judged5')
	judged_percent = judged_metric['diff']['percent'] if eval_threshold != -1 else -1
	need_evaluate = abs(judged_percent) > float(eval_threshold) if eval_threshold != -1 else False

	result = {
		  'name': name
		, 'query_set': query_set
		, 'link': link
		, 'region': region
		, 'leftValue': left_value
		, 'rightValue': right_value
		, 'percent_delta': percent_delta
		, 'pValue': pValue
		, 'color': color
		, 'result': result
		, 'serp_id': beta_serp_id
		, 'judged_percent': judged_percent
		, 'need_evaluate': need_evaluate
	}

	return result

def main():

	parser = argparse.ArgumentParser(description = "ice prosector script")
	parser.add_argument("--dump-metrics-dir", dest = "dump_dir", required = True)
	parser.add_argument("--skip-errors", dest = "skip_errors", action = "store_true")
	args = parser.parse_args(sys.argv[1:])

	'''
	(max_percent_delta, pvalue_min)
	'''
	default_limits = [
		  PercentDiffTest( -0.5, 0.95, 'warning')
		, PercentDiffTest( -1.0, 0.0,  'warning')
		, PercentDiffTest( -1.0, 0.95, 'failed')
		, PercentDiffTest( -3.0, 0.0,  'failed')
	]

	leakage_limits = [
		  LeakagePercentDiffTest( 7.0, 0.95, 'warning')
		, LeakagePercentDiffTest(10.0,  0.0, 'warning')
		, LeakagePercentDiffTest(15.0, 0.95, 'failed')
		, LeakagePercentDiffTest(20.0,  0.0, 'failed')
	]

	music_limits = leakage_limits

	small_serp = [
		  MetricValueTest(lambda x: x != 0, 'failed')
	]

	component_count = [
		  MetricValueTest(lambda x: x < 30, 'warning')
		, MetricValueTest(lambda x: x < 20, 'failed')
	]

	'''evaluation threshold'''
	eval_threshold = 2
	eval_ignore = -1

	test_cases = [
		  ('judged-video-normalized-p5', 'validate', 'RU', default_limits, eval_threshold)
		, ('judged-video-normalized-p10', 'validate', 'RU', default_limits, eval_threshold)
		, ('judged-pfound-5', 'validate', 'RU', default_limits, eval_threshold)
		, ('judged-pfound-10', 'validate', 'RU', default_limits, eval_threshold)

		, ('judged-video-normalized-p5', 'validate', 'TR', default_limits, eval_threshold)
		, ('judged-video-normalized-p10', 'validate', 'TR', default_limits, eval_threshold)
		, ('judged-pfound-5', 'validate', 'TR', default_limits, eval_threshold)
		, ('judged-pfound-10', 'validate', 'TR', default_limits, eval_threshold)

		, ('judged-video-normalized-p5', 'validate', 'BY', default_limits, eval_threshold)
		, ('judged-video-normalized-p10', 'validate', 'BY', default_limits, eval_threshold)
		, ('judged-pfound-5', 'validate', 'BY', default_limits, eval_threshold)
		, ('judged-pfound-10', 'validate', 'BY', default_limits, eval_threshold)

		, ('judged-video-normalized-p5', 'validate', 'KZ', default_limits, eval_threshold)
		, ('judged-video-normalized-p10', 'validate', 'KZ', default_limits, eval_threshold)
		, ('judged-pfound-5', 'validate', 'KZ', default_limits, eval_threshold)
		, ('judged-pfound-10', 'validate', 'KZ', default_limits, eval_threshold)

		, ('judged-video-normalized-p5', 'validate', 'UA', default_limits, eval_threshold)
		, ('judged-video-normalized-p10', 'validate', 'UA', default_limits, eval_threshold)
		, ('judged-pfound-5', 'validate', 'UA', default_limits, eval_threshold)
		, ('judged-pfound-10', 'validate', 'UA', default_limits, eval_threshold)

		, ('judged-video-normalized-p5', 'validate', 'ID', default_limits, eval_threshold)
		, ('judged-video-normalized-p10', 'validate', 'ID', default_limits, eval_threshold)
		, ('judged-pfound-5', 'validate', 'ID', default_limits, eval_threshold)
		, ('judged-pfound-10', 'validate', 'ID', default_limits, eval_threshold)

		, ('judged-video-normalized-p5', 'porno', 'RU', default_limits, eval_threshold)
		, ('judged-video-normalized-p10', 'porno', 'RU', default_limits, eval_threshold)

		, ('judged-video-normalized-p5', 'music', 'RU', default_limits, eval_threshold)
		, ('judged-video-normalized-p10', 'music', 'RU', default_limits, eval_threshold)

		, ('judged-video-mobile-pfound-5', 'mobile-validate', 'RU', default_limits, eval_threshold)
		, ('judged-video-mobile-5', 'mobile-validate', 'RU', default_limits, eval_threshold)

		, ('judged-video-normalized-p5', 'mobile-validate', 'TR', default_limits, eval_threshold)
		, ('judged-video-normalized-p10', 'mobile-validate', 'TR', default_limits, eval_threshold)

		, ('adult-10', 'leakage', 'RU', leakage_limits, eval_ignore)
		, ('adult-10', 'leakage', 'TR', leakage_limits, eval_ignore)
		, ('adult-20', 'leakage', 'RU', leakage_limits, eval_ignore)
		, ('adult-20', 'leakage', 'TR', leakage_limits, eval_ignore)

		, ('component-count', 'videotop', 'RU', component_count, eval_ignore)
		, ('small-serp', 'videotop', 'RU', small_serp, eval_ignore)

		, ('judged-video-normalized-p5', 'serial', 'RU', default_limits, eval_threshold)
		, ('judged-video-normalized-p10', 'serial', 'RU', default_limits, eval_threshold)
		, ('judged-pfound-5', 'serial', 'RU', default_limits, eval_threshold)
		, ('judged-pfound-10', 'serial', 'RU', default_limits, eval_threshold)

		, ('judged-video-normalized-p5', 'serial', 'TR', default_limits, eval_threshold)
		, ('judged-video-normalized-p10', 'serial', 'TR', default_limits, eval_threshold)
		, ('judged-pfound-5', 'serial', 'TR', default_limits, eval_threshold)
		, ('judged-pfound-10', 'serial', 'TR', default_limits, eval_threshold)
	]

	downloaded_serps = serps.Serps(args.dump_dir)

	report_json = {'Tests': []}
	for test in test_cases:
		try:
			report_json['Tests'].append(run_test(downloaded_serps, test))
		except:
			if not args.skip_errors:
				raise

	output_file = open("{0}/{1}".format(args.dump_dir, 'report.json'), 'w')
	output_file.write(json.dumps(report_json))
	output_file.close()

if __name__ == "__main__":
	main()

