#!/usr/bin/perl

use warnings;
use strict;

use Test::More;
use Test::MockTime qw/:all/;

use MailService;
use Yandex::SendSMS;
use ShardingTools qw/ppc_shards/;

use Settings;
use Yandex::DBTools;
use Yandex::DBUnitTest qw/:all/;


use utf8;
use open ':std' => ':utf8';

my $timezone = 3;
my @ppc_shards = map {/:(\d+)(:|$)/; $1} dbnames(PPC(shard => 'all'));

# проверочный хеш: час отправки => число отправленных смс и состояние очереди
my %check = (
    '02'    => {
        count   => 4,
        rows    => [
            { sms_id => 1, send_status => 'Send' },
            { sms_id => 2, send_status => 'Send' },
            { sms_id => 3, send_status => 'Send' },
            { sms_id => 4, send_status => 'Send' },
        ],
    },
    '05'    => {
        count => 3,
        rows => [
            { sms_id => 1, send_status => 'Send' },
            { sms_id => 2, send_status => 'Wait' },
            { sms_id => 3, send_status => 'Send' },
            { sms_id => 4, send_status => 'Send' },
        ],
    },
    '15'    => {
        count => 2,
        rows => [
            { sms_id => 1, send_status => 'Wait' },
            { sms_id => 2, send_status => 'Wait' },
            { sms_id => 3, send_status => 'Send' },
            { sms_id => 4, send_status => 'Send' },
        ],
    },
);

Test::More::plan(tests => 1 + (2 * (scalar keys %check) * scalar @ppc_shards));

#Yandex::SendSMS::FAKESMS = "file:$Settings::LOG_ROOT/fakesms.log";
$Yandex::SendSMS::FAKESMS = "null";
ok( $Yandex::SendSMS::FAKESMS, 'Settind fake sms log' );

SKIP: {

    skip 'LOG_ROOT directory does not exists', (2 * (scalar keys %check) * scalar @ppc_shards) unless -d $Yandex::Log::LOG_ROOT;

    foreach my $shard (@ppc_shards) {
        # остальные шарды делаем пустыми
        my @other_shards = map { $_ => [] } grep { $_ != $shard } @ppc_shards;

        my $dataset = {
            campaigns => {
                original_db => PPC(shard => 'all'),
                rows => {
                    $shard => [
                        { cid =>  1 },
                        { cid =>  2 },
                        { cid =>  3 },
                        { cid =>  4 },
                    ],
                    @other_shards,
                },
            },
            camp_options => {
                original_db => PPC(shard => 'all'),
                rows => {
                    $shard => [
                        {
                            cid         => 1,
                            sms_flags   => 'active_orders_money_out_sms,active_orders_money_warning_sms',
                            sms_time    => '01:00:09:00',
                        }, {
                            cid         => 2,
                            sms_flags   => 'active_orders_money_out_sms,active_orders_money_warning_sms',
                            sms_time    => '18:00:03:00',
                        }, {
                            cid         => 3,
                            sms_flags   => 'active_orders_money_out_sms,active_orders_money_warning_sms',
                            sms_time    => '18:45:18:45',   # круглосуточно
                        }, {
                            cid         => 4,
                            sms_flags   => 'active_orders_money_out_sms,active_orders_money_warning_sms',
                            sms_time    => '00:00:00:00',   # круглосуточно
                        },
                    ],
                    @other_shards,
                },
            },
            sms_queue => {
                original_db => PPC(shard => 'all'),
                rows => {
                    $shard => [
                        {
                            sms_id          => 1,
                            uid             => 1,
                            cid             => 1,
                            sms_text        => 'У попа была собака',
                            send_status     => 'Wait',
                            template_name   => 'active_orders_money_out_sms',
                        }, {
                            sms_id          => 2,
                            uid             => 1,
                            cid             => 2,
                            sms_text        => 'Он её любил',
                            send_status     => 'Wait',
                            template_name   => 'active_orders_money_out_sms',
                        }, {
                            sms_id          => 3,
                            uid             => 1,
                            cid             => 3,
                            sms_text        => 'Она съела кусок мяса',
                            send_status     => 'Wait',
                            template_name   => 'active_orders_money_out_sms',
                        }, {
                            sms_id          => 4,
                            uid             => 1,
                            cid             => 4,
                            sms_text        => 'Он её убил',
                            send_status     => 'Wait',
                            template_name   => 'active_orders_money_out_sms',
                        },
                    ],
                    @other_shards,
                },
            },
            shard_uid => {
                original_db => PPCDICT,
            },
        };

        for my $hour ( sort keys %check ) {
            init_test_dataset($dataset);

            set_fixed_time('01/01/2010 '.sprintf('%02d',($hour+24-$timezone)%24).':00:00', '%m/%d/%Y %H:%M:%S');
            my $sms_count = MailService::process_sms_queue(shard => $shard);

            is( $sms_count, $check{$hour}->{count}, "shard $shard: At $hour:00 - sending $check{$hour}->{count} sms" );
            check_test_dataset(
                {
                    sms_queue => {
                        original_db => PPC(shard => 'all'),
                        rows => {
                            $shard => $check{$hour}->{rows},
                            @other_shards,
                        }
                    }
                },
                'check sms_queue in database'
            );
        }
        
    }


}
