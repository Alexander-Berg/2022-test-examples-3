#!/usr/bin/perl

# $Id$

use warnings;
use strict;
use utf8;

use Test::More;

use Yandex::Test::UTF8Builder;

use BS::Export::Queues;

use constant {
    MIN_PAR_ID => 0,
    MAX_PAR_ID => 255,
};

my $Q = \%BS::Export::Queues::QUEUES;
my $S = \%BS::Export::Queues::SPECIAL_PAR_TYPES;

for my $par_id (keys %$Q) {
    my $par_name = sprintf('par-id: %d (%s)', $par_id, ($Q->{$par_id}->{desc} // 'no description'));
    ok(MIN_PAR_ID <= $par_id && $par_id <= MAX_PAR_ID, "$par_name - номер потока в допустимом диапазоне значений");
}

my $PAR_ID2SPECIAL_TYPE = { reverse(%$S) };
ok(scalar(keys(%$S)) == scalar(keys(%$PAR_ID2SPECIAL_TYPE)), 'В %SPECIAL_PAR_TYPES нет дубликатов');

for my $par_id (keys %$PAR_ID2SPECIAL_TYPE) {
    my $par_name = $PAR_ID2SPECIAL_TYPE->{$par_id};
    ok(MIN_PAR_ID <= $par_id && $par_id <= MAX_PAR_ID, "$par_name - номер потока в допустимом диапазоне значений");
    ok(!exists $Q->{ $par_id }, "$par_name par-id не используется для очередей экспорта");
}

done_testing();
