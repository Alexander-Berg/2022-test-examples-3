#!/usr/bin/perl

use Direct::Modern;

use Test::Intapi;

use JSON;
use Encode;

my @tests = (
    {
        name => 'simple query',
        read_only => 1,
        url => base_url().'/Notification',
        method => 'POST',
        data => [
            {
                name => 'notify_order_money_in_wo_eventlog',
                vars => {
                    agency_email => undef,
                    agency_uid => undef,
                    camp_name => 'Test Wallet',
                    campaign_id => 8894660,
                    campaign_type => 'wallet',
                    cid => 8894660,
                    client_country_region_id => 225,
                    client_email => 'ZX128k@yandex.ru',
                    client_fio => 'Test Test',
                    client_id => 4338546,
                    client_login => 'zx128k',
                    client_phone => undef,
                    client_uid => 16040823,
                    currency => 'RUB',
                    easy_camp => 0,
                    fio => 'Test Test',
                    in_future => 0,
                    pay_type => 'any',
                    start_time_ts => '1396555200',
                    sum => 10000,
                    sum_payed => '8474.57627118644',
                    sum_payed_original => '10000.00',
                    sum_payed_units_rate => 10000,
                    wallet_cid => 0,
                    without_nds => 1
                },
                options => {'mail_fio_tt_name' => 'client_fio'},
            },
        ],
        preprocess => sub { return encode 'utf8', to_json shift },
        check_num => 2,
        check => sub {
            my ($data, $resp, $name) = @_;
            my $response;
            lives_ok { $response = $resp->content } "$name: got response";
            my %expected_result;
            cmp_deeply({$response}, {''}, "$name: good answer");
        }
    },
);

run_tests(\@tests);

