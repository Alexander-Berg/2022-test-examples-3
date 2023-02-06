#!/usr/bin/perl

use warnings;
use strict;
use Test::More tests => 15;

use Mediaplan qw/is_first_phrase_winner_by_ctr/;
use TextTools qw/round2s/;
use utf8;
use open ':std' => ':utf8';

my $ph_text1 = "золотой теленок -ильф -петров";
my $ph_text2 = "золотой теленок -скачать -онлайн -приложение";

my ($p_ctr, $ctr, $pectr, $ectr) = (round2s(9.066667), round2s(0.777778), round2s(0.07855), round2s(0.019629));
my ($p_ctr_short, $ctr_short, $pectr_short, $ectr_short) = (9.06, 0.77, 0.07, 0.01);

sub ph {
	return {p_ctr=>$_[0], ctr=>$_[1], pectr=>$_[2], ectr=>$_[3], phrase=>$_[4]};
}

my @tests = ([[$p_ctr, $ctr, $pectr, $ectr, $ph_text1], [$p_ctr-1, $ctr, $pectr, $ectr, $ph_text2], 1, 'The first phrase with a bigger p_ctr'],
			 [[$p_ctr, $ctr, $pectr, $ectr, $ph_text1], [$p_ctr+1, $ctr, $pectr, $ectr, $ph_text2], 0, 'The second phrase with a bigger p_ctr'],
			 [[$p_ctr_short, $ctr, $pectr, $ectr, $ph_text1], [$p_ctr, $ctr, $pectr, $ectr, $ph_text2], 0, 'The second phrase with a bigger p_ctr (check round2s)'],

			 [[$p_ctr, $ctr+1, $pectr, $ectr, $ph_text1], [$p_ctr, $ctr, $pectr, $ectr, $ph_text2], 1, 'The first phrase with a bigger ctr'],
			 [[$p_ctr, $ctr, $pectr, $ectr, $ph_text1], [$p_ctr, $ctr+1, $pectr, $ectr, $ph_text2], 0, 'The second phrase with a bigger ctr'],
			 [[$p_ctr, $ctr_short, $pectr, $ectr, $ph_text1], [$p_ctr, $ctr, $pectr, $ectr, $ph_text2], 0, 'The second phrase with a bigger ctr (check round2s)'],

			 [[$p_ctr, $ctr, $pectr, $ectr, $ph_text1], [$p_ctr, $ctr, $pectr, $ectr, $ph_text2], 0, 'The second phrase with a bigger number of minus words'],
			 [[$p_ctr, $ctr, $pectr, $ectr, $ph_text2], [$p_ctr, $ctr, $pectr, $ectr, $ph_text1], 1, 'The first phrase with a bigger number of minus words'],

			 [[$p_ctr, $ctr, $pectr+1, $ectr, $ph_text1], [$p_ctr, $ctr, $pectr, $ectr, $ph_text1], 1, 'The first phrase with a bigger pectr'],
			 [[$p_ctr, $ctr, $pectr, $ectr, $ph_text1], [$p_ctr, $ctr, $pectr+1, $ectr, $ph_text1], 0, 'The second phrase with a bigger pectr'],
			 [[$p_ctr, $ctr, $pectr_short, $ectr, $ph_text1], [$p_ctr, $ctr, $pectr, $ectr, $ph_text1], 0, 'The second phrase with a bigger pectr (check round2s)'],

			 [[$p_ctr, $ctr, $pectr, $ectr+1, $ph_text1], [$p_ctr, $ctr, $pectr, $ectr, $ph_text1], 1, 'The first phrase with a bigger ectr'],
			 [[$p_ctr, $ctr, $pectr, $ectr, $ph_text1], [$p_ctr, $ctr, $pectr, $ectr+1, $ph_text1], 0, 'The second phrase with a bigger ectr'],
			 [[$p_ctr, $ctr, $pectr, $ectr_short, $ph_text1], [$p_ctr, $ctr, $pectr, $ectr, $ph_text1], 0, 'The second phrase with a bigger ectr (check round2s)'],

			 [[$p_ctr, $ctr, $pectr, $ectr, $ph_text1], [$p_ctr, $ctr, $pectr, $ectr, $ph_text1], 0, 'Totally the same phrases'],
			);

foreach my $test (@tests) {
	is (is_first_phrase_winner_by_ctr(ph(@{$test->[0]}), ph(@{$test->[1]})), $test->[2], $test->[3]);
}


1;
