#!/usr/bin/perl

use strict;
use warnings;

use Yandex::Test::UTF8Builder;
use Test::More;

use GeoTools;

use utf8;

*ptm = sub {
    my ($reg_list, $opts) = @_;
    my @list = split /,/, GeoTools::convert_plus_reg_list_to_mixed_geo($reg_list, $opts);
    # для удобства сравнения все группы минус-регионов сортируем по возрастанию абсолютного значения
    my @minus_buf;
    my @sorted_list = ();
    while (defined (my $reg = shift @list)) {
    	push @minus_buf, $reg if $reg < 0;
    	push @sorted_list, sort { abs($a) <=> abs($b) } @minus_buf if ($reg >= 0 || !@list);
    	if ($reg >= 0) {
    		@minus_buf = ();
    		push @sorted_list, $reg;
    	}
    }
    return join ',', @sorted_list;
};

sub childs {
	my ($parent, $exceptions, $opts) = @_;
	$exceptions ||= [];
	my %ex = map { $_ => 1 } @$exceptions;

	return grep { !$ex{$_} } @{GeoTools::get_translocal_region($parent, $opts)->{childs}};
}

my $opts = {tree => 'ru'};
is(ptm(['!225',childs(225,[3],$opts),1], $opts), '225,-3,1', 'Россия -Центр Москва');

is(ptm(['!225',childs(225,[3],$opts)], $opts), '225,-3', 'Россия -Центр');

is(ptm(['!225','!17','!3',
        childs(225,[3,17],$opts),
		childs(3,[1], $opts),
		childs(17,[10174], $opts)], $opts), '225,-1,-10174', 'Россия -Москва -Питер');

is(ptm(['!225','!3',
        childs(225,[3],$opts),
		childs(3,[1], $opts),
        '!187',
		childs(187,[20544], $opts)], $opts), '187,-20544,225,-1', 'Украина -Киев Россия -Москва');

is(ptm([0], $opts), '0', 'Весь мир');

is(ptm([], $opts), '0', 'пустой список');

done_testing();
