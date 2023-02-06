#!/usr/bin/perl

=head1 DESCRIPTION

    Поле type у каждого события в %EventLog::EVENTS должно влезать в users_notifications_details.event_type

=cut

use strict;
use warnings;
use utf8;

use Test::More tests => 2;
use Yandex::Test::UTF8Builder;

BEGIN { use_ok( 'EventLog', '%EVENTS' ); }

my $failed;
for my $slug (keys %EventLog::EVENTS) {
    if (length $slug > 30) {
        diag("У события $slug превышен размер имени");
        $failed = 1;
        last;
    }
}
ok(!$failed, 'размер имени события');
