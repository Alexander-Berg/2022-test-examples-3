#!/usr/bin/perl

use warnings;
use strict;
use Test::More;

use Test::Deep;

use RedirectCheckQueue;

use Settings;
use Yandex::DBUnitTest qw/:all/;
use Yandex::Test::UTF8Builder;

use utf8;
use open ':std' => ':utf8';

my %db = (
    trusted_redirects => {
        original_db => PPCDICT,
        like => 'trusted_redirects',
        rows => [
            {
                domain => 'moto.auto.ru',
                redirect_type => 'counter',
            },
            {
                domain => 'logp3.xiti.com',
                redirect_type => 'counter',
            },
        ],
    },
);

init_test_dataset(\%db);

my $d  = 'yandex.ru';

sub banner {
	return {href=>$_[0], domain=>$_[1]};
}

my $old_banner_array = 

my @tests = (
	[	
		{href =>"http://moto.auto.ru/carting/"},
	 	["moto.auto.ru", 1], 
	 	"3-level domain (with http:// )"
	],
	[	
		{href =>"moto.auto.ru/carting/"},
	 	["moto.auto.ru", 1], 
	 	"3-level domain (without http:// )"
	],
	[	
		{href =>"moto.auto.ru", old_banner=>"moto.auto.ru"},
	 	["moto.auto.ru", 0], 
	 	"3-level domain"
	],
	[	
		{href =>"auto.ru/daewoo/"},
	 	["auto.ru", 0], 
	 	"2-level domain"
	],
	[	
		{href =>"www.auto.ru/daewoo/"},
	 	["www.auto.ru", 0], 
	 	"2-level domain with www"
	],
	[	
		{href =>"http://goo.gl/VQ5T1"},
	 	["goo.gl", 0], 
	 	"2-level untrusted shoter"
	],
	[	
		{href =>"http://logp3.xiti.com/hit.xiti?s=388760&s2=1&p=Homepage"},
	 	["logp3.xiti.com", 1], 
	 	"Trusted shoter"
	],
	[
        # нового href нет
       {old_banner => 'https://kiev.ko.olx.ua/obyavlenie/arenda-prokat-orbitreka-christopeit-al-5-IDdeh7F.html'},
       ['', 0]
	]
	);


Test::More::plan(tests => scalar (@tests));

# Проверка на на дубликаты без учета домена и региона
foreach my $test (@tests) {
	my $banner = banner($test->[0]->{href});
	my $old_banner = banner($test->[0]->{old_banner} || $d, $test->[0]->{old_banner} || $d);

	my $result = [RedirectCheckQueue::domain_need_check_redirect($banner, $old_banner)];
 	is_deeply ($result, $test->[1], $test->[2]);
}

1;
