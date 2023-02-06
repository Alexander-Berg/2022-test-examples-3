#!/usr/bin/perl

=head1 DESCRIPTION

    Проверяет наличие сроков хранения в %EventLog::CLEAR_EVENTS_AFTER_DAYS у всех событий из %EventLog::EVENTS

=cut

use strict;
use warnings;
use utf8;

use Test::More tests => 3;
use Yandex::Test::UTF8Builder;

BEGIN { use_ok( 'EventLog', '%CLEAR_EVENTS_AFTER_DAYS' ); }

my $have_clear_after = 1;
for my $slug (keys %EventLog::EVENTS) {
    if (!exists $EventLog::CLEAR_EVENTS_AFTER_DAYS{$slug}) {
        diag("Не указан срок хранения для события $slug");
        $have_clear_after = 0;
        last;
    }
}
ok($have_clear_after, 'наличие сроков хранения у всех событий');

my $have_no_unknown_events = 1;
for my $slug (keys %EventLog::CLEAR_EVENTS_AFTER_DAYS) {
    if (!exists $EventLog::EVENTS{$slug}) {
        diag("Указан срок хранения для неизвестного события $slug");
        $have_no_unknown_events = 0;
        last;
    }
}
ok($have_no_unknown_events, 'сроки хранения для реальных событий');
