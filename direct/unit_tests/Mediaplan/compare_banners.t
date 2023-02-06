#!/usr/bin/perl

use warnings;
use strict;
use Test::More tests => 13;

use Mediaplan qw/compare_banners/;
use utf8;
use open ':std' => ':utf8';

my $title = "Шерсть альпаки";
my $body = "Получаются очень теплые изделия";
my $href =  'http://ya.ru';
my $geo = '255';
my $phr = "пряжа";
sub banner {
	return {} unless @_;
	return {title=>$_[0], body=>$_[1], href=>$_[2], geo=>$_[3], Phrases=>[map {ph($_)} @{$_[4]}]};
}

sub ph {
	return {phrase=>$_->[0], id=>$_->[1]};
}

my @tests = ([ [$title, $body, $href, $geo, [["комната", 1]]], [$title, $body, $href, $geo, [["комната", 1]]], {}, "None changes"],

	         [ [$title, $body, $href, $geo, [["стол", 1]]], [$title, $body, $href, $geo, [["столы", 1]]], {phrases=>{1=>'changed'}}, "One phrase edited"],

	         [ [$title, $body, $href, $geo, [["красивая лампа", 1]]], [$title, $body, $href, $geo, [["зеленая лампа", 1]]], {phrases=>{1=>'added'}}, "One phrase added"],

	         [ [$title, $body, $href, $geo, [["красивая лампа на столе", 1]]], [$title, $body, $href, $geo, [["красивая лампа", 1]]], {phrases=>{1=>'changed'}}, "removed phrase words"],

	         [ [$title, $body, $href, $geo, [["красивая лампа", 1]]], [$title, $body, $href, $geo, [["красивая лампа на столе", 1]]], {phrases=>{1=>'added'}}, "added phrase words"],

	         [ [$title, $body, $href, $geo, [[$phr, 1]]], ["Овечья шерсть", $body, $href, $geo, [[$phr, 1]]], {title=>1}, "title edited"],

	         [ [$title, $body, $href, $geo, [[$phr, 1]]], [$title, "Теплые носки", $href, $geo, [[$phr, 1]]], {body=>1}, "body edited"],

	         [ [$title, $body, $href, $geo, [[$phr, 1]]], ["Овечья шерсть", "Теплые носки", $href, $geo, [[$phr, 1]]], {title=>1, body=>1}, "title and body edited"],

	         [ [$title, $body, $href, $geo, [["стол", 1]]], ["Овечья шерсть", "Теплые носки", $href, $geo, [["столы", 1]]], 
	           {title=>1, body=>1, phrases=>{1=>'changed'}}, "title and body and phrase edited"],

	         [ [$title, $body, $href, $geo, [["красивая лампа", 1], ["красная поляна", 2]]], [$title, $body, $href, $geo, [["красивой лампе", 1], ["красной поляне", 2]]], 
	           {phrases=>{1=>'changed', 2=>'changed'}}, "Two phrases changed"],

	         [ [$title, $body, $href, $geo, [["красивая лампа", 1], ["красная поляна", 3]]], [$title, $body, $href, $geo, [["красивой лампе", 1], ["красной поляне", 2]]], 
	           {phrases=>{1=>'changed', 3=>'added'}}, "Both: added and changed 1"],

	         [ [$title, $body, $href, $geo, [["красивая лампа", 1], ["красная поляна", 2]]], [$title, $body, $href, $geo, [["красивой лампе", 1], ["зеленый шмель", 2]]], 
	           {phrases=>{1=>'changed', 2=>'added'}}, "Both: added and changed 2"],

	         [ [$title, $body, $href, $geo, [[$phr, 1]]], [], {phrases=>{1=>'added'}}, "Whole banner was added"],
	        );


foreach my $test (@tests) {
	is_deeply (compare_banners(banner(@{$test->[0]}), banner(@{$test->[1]})), $test->[2], $test->[3]);

}


1;
