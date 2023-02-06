#!/usr/bin/perl

use warnings;
use strict;
use Test::More;

use Test::Deep;

use PhraseDoubles qw/get_all_doubles/;

use Settings;
use Yandex::DBUnitTest qw/init_test_dataset/;
use utf8;
use open ':std' => ':utf8';
$Yandex::DBShards::STRICT_SHARD_DBNAMES = 0;

my $d  = 'ya.ru';
my $d2 = 'ya.com';
my $g  = '255';
my $g2 = '253';

sub banner {
	my @p = @_;
	return {cid=>$p[0], mbid=>$p[1], domain=>$p[2], geo=>$p[3], Phrases=>[map {ph($p[0], $p[1], @{$_})} @{$_[4]}]};
}

sub ph {
	return {phrase=>$_[0], id=>$_[1], bid=>$_[2]};
}

sub ph_empty {
	return {phrase=>$_};
}

my %db = (
    mediaplan_banners => {
    	original_db => PPC,
    	rows => [],
    },
    mediaplan_bids => {
        original_db => PPC,
        rows => [],
    },
);

my @tests = (
	[	
		{phrases1 =>[["белый", 1], ["белое", 2]], phrases2 => [["белая"],["белые"]]},
	 	{"белая"=>[["белая"], ["белые"], ["белый", 1], ["белое", 2]]}, 
	 	"Same formas, diff lemmas 1"
	],
	[	
		{phrases1 =>[["купить", 1]], phrases2 => [["купить"]]},
	 	{"купить"=>[["купить"], ["купить", 1]]}, 
	 	"Same formas, diff lemmas 2"
	],
	[	
		{phrases1 =>[["белый", 1], ["белое", 2]], phrases2 => [["белая"],["белый"],["красная"],["красный"]]},
	 	{"белая"=>[["белая"], ["белый"], ["белый", 1], ["белое", 2]],
	 	 "красная"=>[["красная"], ["красный"]]}, 
	 	"Same formas, diff lemmas 3"
	],
	[	
		{phrases1 =>[["белый", 1], ["белое", 2],["красная", 3], ["красный", 4]], phrases2 => [["белая"],["белый"]]},
	 	{"белая"=>[["белая"], ["белый"], ["белый", 1], ["белое", 2]]}, 
	 	"Same formas, diff lemmas 4"
	],
	[	
		{phrases1 =>[["купи", 1]], phrases2 => [["покупать"]]},
	 	{}, 
	 	"Diff words"
	],
	[	
		{phrases1 =>[["белое купи", 1]], phrases2 => [["белая купить"]]},
	 	{"белая купить"=>[["белая купить"], ["белое купи", 1]]}, 
	 	"Several words"
	],
	[	
		{phrases1 =>[["кошка 2 года", 1]], phrases2 => [["кошка года"]]},
	 	{}, 
	 	"Words with numbers"
	],
	[	
		{phrases1 =>[["рукавные разветвления", 1]], phrases2 => [["разветвления рукавные"]]},
	 	{"разветвления рукавные"=>[["разветвления рукавные"], ["рукавные разветвления", 1]]}, 
	 	"Diff word order"
	],
	[	
		{phrases1 =>[["подъемные столы", 1]], phrases2 => [["подъемный стол"]]},
	 	{"подъемный стол"=>[["подъемный стол"], ["подъемные столы", 1]]}, 
	 	"Diff orig formas 1"
	],
	[	
		{phrases1 =>[["ШПО-102", 1], ["ШПО-103", 2]], phrases2 => [["ШПО-105"]]},
	 	{}, 
	 	"Words with minuses and numbers"
	],

	);
my @tests2 = (
	[	
		{phrases =>[["белый", 1011, 101], ["белое", 1012, 101]], bids_domain_geo=>{101=>"${d}_${g}"}},
	 	{"${d}_${g}" => {"белый"=>[["белый", 1011, 101], ["белое", 1012, 101]]}}, 
	 	"Same formas, diff lemmas, the same domain and geo"
	],
	[	
		{phrases =>[["белое", 1011, 101], ["белый", 1012, 101], ["белое", 1013, 102]], bids_domain_geo=>{101=>"${d2}_${g}", 102=>"${d}_${g}"}},
	 	{"${d2}_${g}" => {"белое"=>[["белое", 1011, 101], ["белый", 1012, 101]]}}, 
	 	"Diff domains"
	],
	[	
		{phrases =>[["белое", 1011, 101], ["белый", 1012, 101], ["белое", 1013, 102]], bids_domain_geo=>{101=>"${d}_${g2}", 102=>"${d}_${g}"}},
	 	{"${d}_${g2}" => {"белое"=>[["белое", 1011, 101], ["белый", 1012, 101]]}}, 
	 	"Diff regions"
	],

	);


Test::More::plan(tests => scalar (@tests) + scalar(@tests2));

init_test_dataset(\%db);

# Проверка на на дубликаты без учета домена и региона
foreach my $test (@tests) {
	my $phrases1 = [map {ph(@{$_})} @{$test->[0]->{phrases1}}];
	my $phrases2 = [map {ph(@{$_})} @{$test->[0]->{phrases2}}];

	my $result = {map {my $pp = $_; $pp=>[map {ph(@{$_})} @{$test->[1]->{$pp}}]} keys %{$test->[1]}};
	my $doubles  = get_all_doubles($phrases1, phrases_for_check=>$phrases2);
 	is_deeply ($doubles, $result, $test->[2]);
}

# Проверка на на дубликаты c учетом домена и региона
foreach my $test (@tests2) {
	my $phrases = [map {ph(@{$_})} @{$test->[0]->{phrases}}];
	my $bids_domain_geo = $test->[0]->{bids_domain_geo};

	my $result = $test->[1];
	foreach my $d_g (keys %{$test->[1]}) {
		$result->{$d_g} = { map {my $pp = $_; $pp=>[map {ph(@{$_})} @{$result->{$d_g}->{$pp}}]} keys %{$result->{$d_g}}};
	}
	my $doubles  = get_all_doubles($phrases, bids_domain_geo=>$bids_domain_geo);
	is_deeply ($doubles, $result, $test->[2]);
}


1;
