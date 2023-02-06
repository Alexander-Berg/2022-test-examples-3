#!/usr/bin/perl

=head1 DESCRIPTION

    Поле type у каждого события в %EventLog::EVENTS должно быть уникально

=cut

use strict;
use warnings;
use utf8;

use Test::More tests => 2;
use Yandex::Test::UTF8Builder;

BEGIN { use_ok( 'EventLog', '%EVENTS' ); }

my(%slugs_by_type, $failed);
while (my($slug, $event_attribs) = each %EventLog::EVENTS) {
    my $type = $event_attribs->{type};
    if (exists $slugs_by_type{$type}) {
        diag("У событий $slugs_by_type{$type} и $slug совпадает числовое значение типа события (type)");
        $failed = 1;
        last;
    } else {
        $slugs_by_type{$type} = $slug;
    }
}
ok(!$failed, 'уникальность типа события');
