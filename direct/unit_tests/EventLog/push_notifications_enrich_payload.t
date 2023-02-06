#!/usr/bin/env perl

=head1 DESCRIPTION

Добавление link и category в payload пуша

=cut

use Direct::Modern;

use Test::More;
use Yandex::Test::UTF8Builder;

BEGIN { use_ok('EventLog'); }

my $lp = EventLog::LINK_PREFIX;
my $first_event = {cid => 123, bid => 456};

sub en {
    my $type = shift;
    my $payload = {};
    EventLog::push_notifications_enrich_payload($payload, $type, $first_event);
    return $payload;
}

is_deeply(en('money_out_wallet'), {link => $lp . 'account/payment'}, 'money_out_wallet');
is_deeply(en('money_out'), {link => $lp . 'campaign/123', category => 'PAYMENT_AND_CAMPAIGN'}, 'money_out');
is_deeply(en('money_warning_wallet'), {link => $lp . 'account', category => 'PAYMENT_AND_STATS'}, 'money_warning_wallet');
is_deeply(en('paused_by_day_budget'), {link => $lp . 'campaign/123/dayBudget'}, 'paused_by_day_budget');
is_deeply(en('banner_moderated'), {link => $lp . 'campaign/123/banner/456'}, 'banner_moderated');
is_deeply(en('banner_moderated_multi'), {link => $lp. 'campaign/123'}, 'banner_moderated_multi');
is_deeply(en('custom'), {}, 'custom');

done_testing();
