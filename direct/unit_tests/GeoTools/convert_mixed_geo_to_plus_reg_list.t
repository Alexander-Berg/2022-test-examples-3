#!/usr/bin/perl

use strict;
use warnings;

use Yandex::Test::UTF8Builder;
use Test::More;

use GeoTools;

use utf8;

*mtp = sub {
    my ($geo, $opts) = (@_);
    return list2str(GeoTools::convert_mixed_geo_to_plus_reg_list($geo, $opts));
};


sub list2str {
	my $list = shift;
	return join ',', sort {my $a_int = $a =~ s{^\!}{}r;
						   my $b_int = $b =~ s{^\!}{}r;
						   $a_int <=> $b_int
						   } @{ $list};	
}

sub childs {
	my ($parent, $exceptions, $opts) = @_;
	$exceptions ||= [];
	my %ex = map { $_ => 1 } @$exceptions;

	return grep { !$ex{$_} } @{GeoTools::get_translocal_region($parent, $opts)->{childs}};
}

my $opts = {tree => 'ru'};
is(mtp('225,-3,1', $opts), list2str(['!225',childs(225,[3],$opts),1]), 'Россия -Центр Москва');
is(mtp('225,-1,-3', $opts), list2str(['!225',childs(225,[3],$opts)]), 'Россия -Москва -Центр');
is(mtp('225,-1,17,-10174', $opts), list2str(['!225','!17','!3',
											 childs(225,[3,17],$opts),
											 childs(3,[1], $opts),
											 childs(17,[10174], $opts)]), 'Россия -Москва Северо-Запад -Питер');
is(mtp('225,-1,-10174', $opts), list2str(['!225','!17','!3',
										  childs(225,[3,17],$opts),
										  childs(3,[1], $opts),
										  childs(17,[10174], $opts)]), 'Россия -Москва -Питер');
is(mtp('225,-3', $opts), list2str(['!225',childs(225,[3],$opts)]), 'Россия -Центр');
is(mtp('3', $opts), list2str([3]), 'Центр');
is(mtp('225,-1,187,-20544', $opts), list2str(['!225','!3','!187',
											  childs(225,[3],$opts),
											  childs(3,[1], $opts),
											  childs(187,[20544], $opts),]), 'Россия -Москва Украина -Киев');

is(mtp('', $opts), list2str([0]), 'пустая строка');
is(mtp(undef, $opts), list2str([]), 'undef');

done_testing();
