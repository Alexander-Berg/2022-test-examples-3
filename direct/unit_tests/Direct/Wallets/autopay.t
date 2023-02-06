#!/usr/bin/perl

use Direct::Modern;

use Test::More;
use Test::Deep;

use Yandex::Log;
use Yandex::HashUtils;
use Yandex::DBTools;
use Yandex::DBShards;
use Yandex::DateTime;
use Yandex::TimeCommon;
use Yandex::Test::UTF8Builder;
use Yandex::DBUnitTest qw/:all/;

use Settings;
use Direct::Wallets::Payment;
use Campaign;

{
    no warnings 'redefine';
    *Direct::Wallets::Payment::balance_simple_create_basket = sub {return {status => 'success',
                                                                           trust_payment_id => random_string(10)}};

    *Direct::Wallets::Payment::balance_simple_pay_basket    = sub {return {status => 'wait_for_notification'}};
    *Direct::Wallets::Payment::balance_simple_check_basket  = sub {return {status => 'success'}};
    *Direct::Wallets::Payment::add_notification = sub {};
    *Direct::Wallets::Payment::rbac = sub {return undef};

    *Campaign::create_campaigns_balance = sub { return {balance_res => [0, "Success"]} };

    *Client::ClientFeatures::has_new_autopay_type = sub { return 0 };
    *Client::ClientFeatures::_is_feature_allowed_for_client_ids = sub { my $clientids = shift; return { map { $_ => 0 } @$clientids} };

}

*cap = sub {
    my ($wallet_cid, %O) = @_;
    my ($is_payed, $error) = Direct::Wallets::Payment::check_and_process_wallet_autopay($wallet_cid, %O);
    return $is_payed;
};

*pts = sub {
    my ($wallet_cid, $log, $dont_update_balance_tid) = @_;


    for my $t (@{ Direct::Wallets::Payment::get_incompleted_transactions(1, [$wallet_cid]) }) {
        Direct::Wallets::Payment::get_and_process_transaction_status($t, log => $log);
        update_balance_tid() unless $dont_update_balance_tid;
    }
};

my $wallet_cid = 1;
my $uid = 11;
my $client_nds = 20;

my $dataset = {
    clients => {
        original_db => PPC(shard => 1),
        rows => [
            {ClientID => 1, work_currency => 'RUB'},
        ],
    },
    clients_options => {
        original_db => PPC(shard => 1),
        rows => [],
    },
    client_nds  => {
        original_db => PPC(shard => 1),
        rows => [
            {ClientID => 1, date_from => today(), date_to => today(), nds => $client_nds}
        ],
    },
    users => {
        original_db => PPC(shard => 1),
        rows => [
            {ClientID => 1, uid => 11},
        ],
    },
    campaigns => {
        original_db => PPC(shard => 1),
        rows => [
            {cid => 1, uid => 11, ClientID => 1, currency => 'RUB', archived => 'No', type => 'wallet', statusEmpty => 'No', statusModerate => 'Yes', ProductID => 1 },
            {cid => 2, uid => 11, ClientID => 1, wallet_cid => 1, currency => 'RUB', archived => 'No', type => 'text', statusEmpty => 'No', statusModerate => 'Yes', ProductID => 1 },
        ],
    },
    camp_options => {
        original_db => PPC(shard => 1),
        rows => [
            {cid => 1},
            {cid => 2},
        ],
    },
    wallet_campaigns => {
        original_db => PPC(shard => 1),
        rows => [
            {wallet_cid => 1},
        ],
    },
    autopay_settings => {
        original_db => PPC(shard => 1),
        rows => [],
    },
    wallet_payment_transactions => {
        original_db => PPC(shard => 1),
        rows => [],
    },
    
    shard_client_id => {
        original_db => PPCDICT,
        rows => [
            { ClientID => 1, shard => 1},
        ],
    },
    shard_uid => {
        original_db => PPCDICT,
        rows => [
            { ClientID => 1, uid => 11},
        ],
    },
    shard_inc_cid => {
        original_db => PPCDICT,
        rows => [
            { ClientID => 1, cid => 1},
            { ClientID => 1, cid => 2},
        ],
    },

    products => {
        original_db => PPCDICT,
        rows => [
            { ProductID => 1, EngineID => 1 },
        ],
    },
};

init_test_dataset($dataset);

srand(123);

my $log = $Direct::Wallets::Payment::TLOG = Yandex::Log->new( use_syslog => 0, no_log => 1 );
# раскомментировать если хотим проверить логгирование действий
# $Direct::Wallets::Payment::TLOG = new Yandex::Log(log_file_name => "payment_transactions_unit_test", date_suf => "%Y%m%d", auto_rotate => 1);
# $log = new Yandex::Log(log_file_name => "ppcProcessAutoPayments_unit_test", date_suf => "%Y%m%d", auto_rotate => 1);

# "текущие" настройки автопополнения
my $autopay_settings = {
    wallet_cid => $wallet_cid,
    payer_uid => $uid,
    paymethod_type => 'card',
    paymethod_id => 'card-' . random_string(8),
    remaining_sum => 10,
    payment_sum => 20,
    tries_num => 0,
};

my $totals = {
    sum => 100,
    sum_spent => 91,
};

turn_on_autopay();
apply_autopay_env({}, $totals);

# сценарии ОПЛАТЫ
# нет предыдущих транзакций
ok(cap($wallet_cid, log => $log), 'нет предыдущих транзакций');

# есть завершенная успешная транзакция
create_trans(status => 'Done', balance_status => 'success');
ok(cap($wallet_cid, log => $log), 'есть завершенная успешная транзакция');

# есть завершенная неуспешная транзакия, с самоисправляемой ошибкой, и прошло достаточное количество времени, и кол-во попыток не исчерпано
create_trans(status => 'Error', balance_status => 'error', balance_status_code => 'payment_timeout', create_time__dont_quote => 'NOW() - INTERVAL 6.5*60 MINUTE', _drop_all => 1);
apply_autopay_env({tries_num => 1});
ok(cap($wallet_cid, log => $log), 'есть завершенная неуспешная транзакия, с самоисправляемой ошибкой, и прошло достаточное количество времени, и кол-во попыток не исчерпано');

# есть завершенная неуспешная транзакия, с НЕсамоисправляемой ошибкой, и кол-во попыток равно 0
create_trans(status => 'Error', balance_status => 'error', balance_status_code => 'limit_exceeded');
apply_autopay_env({tries_num => 0});
ok(cap($wallet_cid, log => $log), 'есть завершенная неуспешная транзакия, с НЕсамоисправляемой ошибкой, и кол-во попыток равно 0');

# нет предыдущих транзакций, на кошельке total_sum=0, total_balance_tid=0, и сумма на кампаниях под кошельком =0
drop_all_trans();
apply_autopay_env({}, {sum => 0, total_balance_tid => 0, sum_on_camps => 0});
ok(cap($wallet_cid, log => $log), 'нет предыдущих транзакций, на кошельке total_sum=0, total_balance_tid=0, и сумма на кампаниях под кошельком =0');
apply_autopay_env({}, $totals);

# сценарии НЕОПЛАТЫ
# есть незавершенная транзакция
create_trans(status => 'Processing');
ok(!cap($wallet_cid, log => $log), 'есть незавершенная транзакция');

# есть завершенная неуспешная транзакция, с НЕсамоисправляемой ошибкой
create_trans(status => 'Error', balance_status => 'error', balance_status_code => 'limit_exceeded');
ok(!cap($wallet_cid, log => $log), 'есть завершенная неуспешная транзакция, с НЕсамоисправляемой ошибкой');

# есть завершенная неуспешная транзакция, с НЕсамоисправляемой ошибкой, прошло достаточно времени для самоисправления
create_trans(status => 'Error', balance_status => 'error', balance_status_code => 'limit_exceeded', create_time__dont_quote => 'NOW() - INTERVAL 6.5*60 MINUTE', _drop_all => 1);
apply_autopay_env({tries_num => 1});
ok(!cap($wallet_cid, log => $log), 'есть завершенная неуспешная транзакция, с НЕсамоисправляемой ошибкой, прошло достаточно времени для самоисправления');

# есть завершенная неуспешная транзакция, с самоисправляемой ошибкой, но кол-во попыток превышено
create_trans(status => 'Error', balance_status => 'error', balance_status_code => 'payment_timeout', create_time__dont_quote => 'NOW() - INTERVAL 3 DAY', _drop_all => 1);
apply_autopay_env({tries_num => 5});
ok(!cap($wallet_cid, log => $log), 'есть завершенная неуспешная транзакция, с самоисправляемой ошибкой, но кол-во попыток превышено');

# есть завершенная неуспешная транзакция, с самоисправляемой ошибкой, но достаточное кол-во времени не прошло
create_trans(status => 'Error', balance_status => 'error', balance_status_code => 'payment_timeout', create_time__dont_quote => 'NOW() - INTERVAL 5 HOUR', _drop_all => 1);
apply_autopay_env({tries_num => 1});
ok(!cap($wallet_cid, log => $log), 'есть завершенная неуспешная транзакция, с самоисправляемой ошибкой, но достаточное кол-во времени не прошло');

# есть завершенная успешная транзакция, но неснижаемого порога денег на кошельке не достигли
create_trans(status => 'Done', balance_status => 'success');
apply_autopay_env({}, {sum_spent => 87});
ok(!cap($wallet_cid, log => $log), 'есть завершенная успешная транзакция, но неснижаемого порога денег на кошельке не достигли');
apply_autopay_env({}, {sum_spent => 91});

# нет предыдущих транзакций, на кошельке total_sum=0, total_balance_tid=0, но сумма остатков на кампаниях под кошельком >0
drop_all_trans();
apply_autopay_env({}, {sum => 0, total_balance_tid => 0, sum_on_camps => 10, sum_spent => 5});
ok(!cap($wallet_cid, log => $log), 'нет предыдущих транзакций, на кошельке total_sum=0, total_balance_tid=0, но сумма на кампаниях под кошельком >0');
apply_autopay_env({}, $totals);

# нет предыдущих транзакций, на кошельке total_sum=0, total_balance_tid=0, сумма на кампаниях под кошельком >0, но сумма остатков =0
drop_all_trans();
apply_autopay_env({}, {sum => 0, total_balance_tid => 0, sum_on_camps => 10, sum_spent => 10});
ok(cap($wallet_cid, log => $log), 'нет предыдущих транзакций, на кошельке total_sum=0, total_balance_tid=0, сумма на кампаниях под кошельком >0, но сумма остатков =0');
apply_autopay_env({}, $totals);

###

# tries_num при создании оплаты инкрементится
drop_all_trans();
apply_autopay_env({tries_num => 0});
cap($wallet_cid, log => $log);
is(get_autopay_tries_num(), 1, 'tries_num при создании оплаты инкрементится');

# tries_num после успешного завершения транзакции сбрасывается в 0
pts($wallet_cid, $log);
is(get_autopay_tries_num(), 0, 'tries_num после успешного завершения транзакции сбрасывается в 0');

# tries_num после НЕуспешного завершения транзакции остается без изменений
cap($wallet_cid, log => $log);
my $balance_simple_check_basket_prev = \&Direct::Wallets::Payment::balance_simple_check_basket;
{
    no warnings 'redefine';
    *Direct::Wallets::Payment::balance_simple_check_basket  = sub {return {status => 'error'}};
}
pts($wallet_cid, $log);
is(get_autopay_tries_num(), 1, 'tries_num после НЕуспешного завершения транзакции остается без изменений');
{
    no warnings 'redefine';
    *Direct::Wallets::Payment::balance_simple_check_basket  = $balance_simple_check_basket_prev;
}

# tries_num после НЕуспешного создания транзакции устанавливается в -1
drop_all_trans();
my $balance_simple_create_basket_prev = \&Direct::Wallets::Payment::balance_simple_create_basket;
{
    no warnings 'redefine';
    *Direct::Wallets::Payment::balance_simple_create_basket  = sub {return {status => 'error'}};
}
ok(!cap($wallet_cid, log => $log), 'неуспешное создание транзакции');
is(get_autopay_tries_num(), -1, 'tries_num после НЕуспешного создания транзакции устанавливается в -1');

# при tries_num = -1 оплату не производим
{
    no warnings 'redefine';
    *Direct::Wallets::Payment::balance_simple_create_basket  = $balance_simple_create_basket_prev;
}
ok(!cap($wallet_cid, log => $log), 'при tries_num = -1 оплату не производим');

# проверяем логику про дожидание нотификации после успешной транзакции
drop_all_trans();
cap($wallet_cid, log => $log);
pts($wallet_cid, $log, 'dont_update_balance_tid');
ok(!cap($wallet_cid, log => $log), 'если total_balance_tid не изменился - оплату не производим');
update_balance_tid();
ok(cap($wallet_cid, log => $log), 'если total_balance_tid изменился - оплату производим');

done_testing();

sub turn_on_autopay {
    do_update_table(PPC(cid => $autopay_settings->{wallet_cid}), 'wallet_campaigns', {autopay_mode => 'min_balance'},
                                                                            where => {wallet_cid => SHARD_IDS});
}

sub turn_off_autopay {
    do_update_table(PPC(cid => $autopay_settings->{wallet_cid}), 'wallet_campaigns', {autopay_mode => 'none'},
                                                                            where => {wallet_cid => SHARD_IDS});
}

sub apply_autopay_env {
    my ($opts_to_change, $total_sums) = @_;
    $opts_to_change //= {};
    
    hash_merge($autopay_settings, $opts_to_change);
    my $wallet_cid = $autopay_settings->{wallet_cid};

    do_insert_into_table(PPC(cid => $wallet_cid), 'autopay_settings', $autopay_settings, on_duplicate_key_update => 1, key => 'wallet_cid');
    if ($total_sums) {
        if (defined $total_sums->{sum}) {
            do_update_table(PPC(cid => $wallet_cid), 'wallet_campaigns', {total_balance_tid => $total_sums->{total_balance_tid} // 1,
                                                                          total_sum => $total_sums->{sum}},
                                                                where => {wallet_cid => SHARD_IDS});
        }
        my $cids_under_wallet = get_one_column_sql(PPC(cid => $wallet_cid), ["select cid 
                                                                                from campaigns",
                                                                               where => {wallet_cid => $wallet_cid}]);
        if (defined $total_sums->{sum_on_camps}) {
            my $sum_single_camp = $total_sums->{sum_on_camps} / scalar(@$cids_under_wallet);
            do_update_table(PPC(cid => $cids_under_wallet), 'campaigns', {sum => $sum_single_camp},
                                                                where => {cid => SHARD_IDS});
        }
        if (defined $total_sums->{sum_spent}) {
            my $sum_spent_single_camp = $total_sums->{sum_spent} / scalar(@$cids_under_wallet);
            do_update_table(PPC(cid => $cids_under_wallet), 'campaigns', {sum_spent => $sum_spent_single_camp},
                                                                where => {cid => SHARD_IDS});
        }
    }
}

sub get_autopay_tries_num {
    return get_one_field_sql(PPC(cid => $wallet_cid), ['select tries_num from autopay_settings', where => {wallet_cid => SHARD_IDS}]);
}

sub create_trans {
    my %O = @_;

    if (delete $O{_drop_all}) {
        drop_all_trans();
    }
    do_insert_into_table(PPC(cid => $wallet_cid), 'wallet_payment_transactions',
                                                  hash_merge({
                                                                wallet_cid => $wallet_cid,
                                                                payer_uid => $uid,
                                                                trust_payment_id => random_string(10),
                                                                type => 'auto',
                                                             }, \%O) );
    if ($O{status} && $O{status} eq 'Done') {
        apply_autopay_env({tries_num => 0});
    } else {
        apply_autopay_env({tries_num => 1});
    }
}

sub drop_all_trans {
    do_sql(PPC(cid => $wallet_cid), 'delete from wallet_payment_transactions where wallet_cid = ?', $wallet_cid);
    apply_autopay_env({tries_num => 0});
}

sub update_balance_tid {
    do_sql(PPC(cid => $wallet_cid), 'update wallet_campaigns set total_balance_tid = total_balance_tid+1 where wallet_cid = ?', $wallet_cid);
}

sub random_string {
    my $length = shift // 0;
    my @chars = ("A".."Z", "a".."z", 0 .. 9);
    return join '', map { $chars[int(rand(scalar @chars))] } 1..$length;
}
