#!/usr/bin/perl

use Direct::Modern;

use List::MoreUtils qw/none/;
use Readonly;
use Storable qw/dclone/;
use Test::Deep;
use Test::Exception;
use Test::More;

use Yandex::DBTools;
use Yandex::DBUnitTest qw/:all/;
use Yandex::HashUtils;
use Yandex::ListUtils;
use Yandex::Test::UTF8Builder;

use BS::ExportWorker qw/set_sync_campaigns/;
use Settings;

# вместе с Yandex::Test::UTF8Builder, так как он не работает для под-тестов
use open ':std' => ':utf8';

# типы очередей, которые не должны удаляться из specials
# camps_only может удаляться только воркером camps_only, который использует kind = camps_only
Readonly::Hash my %DONT_DELETE_SPECIALS => map { $_ => undef } qw/dev1 dev2 nosend preprod camps_only/;

# Создаем фейковый логгер
my $log = bless({}, 'Yandex::FakeLog');
{
    no warnings 'once';
    *Yandex::FakeLog::out = sub {
        # print STDERR @_, "\n";
    };
}

my $error_logger = sub {};

my @all_kinds = qw/camps_and_data camps_only full/;
my $original_seq_time = '2015-10-12 12:00:00';
my @fields = qw/cid par_id queue_time seq_time sync_val camps_num banners_num contexts_num bids_num prices_num is_full_export/;

my %kind2pair_flag = (
    'camps_and_data' => ['full'],
    'camps_only' => ['data', 'full'],
    'full' => ['data', 'camp'],
);
my %kind2flag = (
    'camps_and_data' => ['data', 'camp'],
    'camps_only' => ['camp'],
    'full' => ['full'],
);
my %kind2fields = (
    camps_and_data => [ grep { $_ =~ m/_num$/ && $_ !~ m/^prices/}  @fields],
    camps_only => [ 'camps_num' ],
    full => [ 'is_full_export' ],
);

my %bs_export_queue = (
    ( map {
        $_ => +{
            cid => $_,
            par_id => 100,
            queue_time => '2014-12-31 23:59:59',
            seq_time => $original_seq_time,
            sync_val => 5,
            camps_num => 1,
            banners_num => 12,
            contexts_num => 11,
            bids_num => 13,
            prices_num => 14,
            is_full_export => 0,
        },
    } (100 .. 109) ),
    ( map {
        $_ => +{
            cid => $_,
            par_id => 1,
            queue_time => '2015-01-01 00:00:00',
            seq_time => $original_seq_time,
            sync_val => 3,
            camps_num => 1,
            banners_num => 3,
            contexts_num => 2,
            bids_num => 4,
            prices_num => 5,
            is_full_export => 0,
        },
    } (1 .. 17) ),
    ( map {
        $_ => +{
            cid => $_,
            par_id => 1,
            queue_time => '2015-01-01 00:00:00',
            seq_time => $original_seq_time,
            sync_val => 3,
            camps_num => 1,
            banners_num => 3,
            contexts_num => 2,
            bids_num => 4,
            prices_num => 5,
            is_full_export => 1,
        },
    } (51 .. 67) ),
    ( map {
        $_ => +{
            cid => $_,
            par_id => 100,
            queue_time => '2014-12-31 23:59:59',
            seq_time => $original_seq_time,
            sync_val => 5,
            camps_num => 1,
            banners_num => 12,
            contexts_num => 11,
            bids_num => 13,
            prices_num => 14,
            is_full_export => 1,
        },
    } (110 .. 119) ),
);


my %bs_export_specials = map { $_->{cid} => $_ } (
    { cid => 101, par_type => 'heavy' },
    { cid => 102, par_type => 'fast' },
    { cid => 103, par_type => 'dev1' },
    { cid => 105, par_type => 'dev2' },
    { cid => 106, par_type => 'nosend' },
    { cid => 107, par_type => 'buggy' },
    { cid => 108, par_type => 'preprod' },
    { cid => 109, par_type => 'camps_only' },
    { cid => 111, par_type => 'heavy' },
    { cid => 112, par_type => 'fast' },
    { cid => 113, par_type => 'dev1' },
    { cid => 115, par_type => 'dev2' },
    { cid => 116, par_type => 'nosend' },
    { cid => 117, par_type => 'buggy' },
    { cid => 118, par_type => 'preprod' },
    { cid => 119, par_type => 'camps_only' },
);

my %db = (
    bs_export_queue => {
        original_db => PPC(shard => 2),
        rows => {
            2 => [ values(%bs_export_queue) ],
        },
    },
    bs_export_specials => {
        original_db => PPC(shard => 2),
        rows => {
            2 => [ values(%bs_export_specials) ],
        },
    },
);
# 'heavy','fast','dev1','begun','dev2','nosend','buggy','preprod'
my %flags = (
    1   => { send_camp => 0, send_data => 0, send_price => 0, send_other => 0, send_full => 0 },
    2   => { send_camp => 0, send_data => 0, send_price => 0, send_other => 1, send_full => 0 },
    3   => { send_camp => 0, send_data => 0, send_price => 1, send_other => 0, send_full => 0 },
    4   => { send_camp => 0, send_data => 0, send_price => 1, send_other => 1, send_full => 0 },
    5   => { send_camp => 0, send_data => 1, send_price => 0, send_other => 0, send_full => 0 },
    6   => { send_camp => 0, send_data => 1, send_price => 0, send_other => 1, send_full => 0 },
    7   => { send_camp => 0, send_data => 1, send_price => 1, send_other => 0, send_full => 0 },
    8   => { send_camp => 0, send_data => 1, send_price => 1, send_other => 1, send_full => 0 },
    9   => { send_camp => 1, send_data => 0, send_price => 0, send_other => 0, send_full => 0 },
    10  => { send_camp => 1, send_data => 0, send_price => 0, send_other => 1, send_full => 0 },
    11  => { send_camp => 1, send_data => 0, send_price => 1, send_other => 0, send_full => 0 },
    12  => { send_camp => 1, send_data => 0, send_price => 1, send_other => 1, send_full => 0 },
    13  => { send_camp => 1, send_data => 1, send_price => 0, send_other => 0, send_full => 0 },
    14  => { send_camp => 1, send_data => 1, send_price => 0, send_other => 1, send_full => 0 },
    15  => { send_camp => 1, send_data => 1, send_price => 1, send_other => 0, send_full => 0 },
    16  => { send_camp => 1, send_data => 1, send_price => 1, send_other => 1, send_full => 0 },
    17  => { send_camp => 0, send_data => 0, send_price => 0, send_other => 0, send_full => 0 },
    51  => { send_camp => 0, send_data => 0, send_price => 0, send_other => 0, send_full => 1 },
    52  => { send_camp => 0, send_data => 0, send_price => 0, send_other => 1, send_full => 1 },
    53  => { send_camp => 0, send_data => 0, send_price => 1, send_other => 0, send_full => 1 },
    54  => { send_camp => 0, send_data => 0, send_price => 1, send_other => 1, send_full => 1 },
    55  => { send_camp => 0, send_data => 1, send_price => 0, send_other => 0, send_full => 1 },
    56  => { send_camp => 0, send_data => 1, send_price => 0, send_other => 1, send_full => 1 },
    57  => { send_camp => 0, send_data => 1, send_price => 1, send_other => 0, send_full => 1 },
    58  => { send_camp => 0, send_data => 1, send_price => 1, send_other => 1, send_full => 1 },
    59  => { send_camp => 1, send_data => 0, send_price => 0, send_other => 0, send_full => 1 },
    60  => { send_camp => 1, send_data => 0, send_price => 0, send_other => 1, send_full => 1 },
    61  => { send_camp => 1, send_data => 0, send_price => 1, send_other => 0, send_full => 1 },
    62  => { send_camp => 1, send_data => 0, send_price => 1, send_other => 1, send_full => 1 },
    63  => { send_camp => 1, send_data => 1, send_price => 0, send_other => 0, send_full => 1 },
    64  => { send_camp => 1, send_data => 1, send_price => 0, send_other => 1, send_full => 1 },
    65  => { send_camp => 1, send_data => 1, send_price => 1, send_other => 0, send_full => 1 },
    66  => { send_camp => 1, send_data => 1, send_price => 1, send_other => 1, send_full => 1 },
    67  => { send_camp => 0, send_data => 0, send_price => 0, send_other => 0, send_full => 1 },
    map { $_ => +{ send_camp => 0, send_data => 0, send_price => 0, send_other => 0, send_full => 0, send_full => 0 } } (100 .. 109, 110 .. 119),
);

sub init {
    BS::ExportWorker::init(
        dbh => get_dbh(PPC(shard => 2)),
        dbh2 => get_dbh(PPC(shard => 2)),
        parid => shift || 1,
        partype => 'fake',
        log => $log,
        error_logger => $error_logger,
    );
    init_test_dataset(\%db);
}

sub get_par_type {
    my $cid = shift;
    return exists $bs_export_specials{$cid} ? $bs_export_specials{$cid}->{par_type} : undef;
}

sub get_test_prefix {
    my $cid = shift;
    return sprintf('[cid %3d, par_id %4s, par_type %10s; camp %d / data %d / price %d / other %d / full_export %d]',
                   $cid,
                   $bs_export_queue{$cid}->{par_id} // 'NULL',
                   get_par_type($cid) // 'NULL',
                   @{ $flags{$cid} }{qw/send_camp send_data send_price send_other send_full/},
                   );
}

sub get_camps {
    my %camps;
    for my $camp (values(%bs_export_queue)) {
        $camps{ $camp->{cid} } = dclone($camp);
        hash_merge($camps{ $camp->{cid} }, $flags{$camp->{cid}});
    }
    return \%camps;
}

sub get_camps_from_db {
    my $fields = join ',', map { sql_quote_identifier($_) } @fields;
    return get_hashes_hash_sql(PPC(shard => 2), "SELECT $fields FROM bs_export_queue");
}

sub get_specials_from_db {
    return get_hash_sql(PPC(shard => 2), 'SELECT cid, par_type FROM bs_export_specials');
}

sub check_specials {
    my %options = @_;
    my $cid = $options{cid};
    my $prefix = $options{test_prefix};
    my $kind = $options{kind};

    my $par_type = get_par_type($cid);
    if (defined $par_type) {
        # kind = camps_only не трогает никаких специальных очередей кроме camps_only
        # а специальную очередь camps_only может сносить только kind = camps_only
        if ($kind ne 'camps_only' && exists $DONT_DELETE_SPECIALS{$par_type}
            || $kind eq 'camps_only' && $par_type ne 'camps_only'
        ) {
            is($options{db_specials}->{$cid}, $par_type, "$prefix запись в bs_export_specials не изменилась (данный par_type не должен удаляться автоматически)");
        } elsif (exists $options{expected_cids}->{$cid}) {
            note("NB! некорректное, но ожидаемое поведение - в SQL-запросе не проверяется $options{need_note}") if $options{need_note};
            is($options{db_specials}->{$cid}, undef, "$prefix кампания удалена из bs_export_specials");
        } else {
            is($options{db_specials}->{$cid}, $par_type, "$prefix запись в bs_export_specials не изменилась");
        }
    } else {
        is($options{db_specials}->{$cid}, undef, "$prefix запись в bs_export_specials отсутствует");
    }
}

# тест 1
dies_ok { set_sync_campaigns({}, fake => []) } 'вызов set_sync_campaigns с неизвестным kind - умирает';

# тесты 2..4
for my $kind (@all_kinds) {
    my $test_name = "вызов set_sync_campaigns [kind = $kind] с пустым списком кампаний";
    my $test_code = sub {
        plan tests => 3 * scalar(keys(%bs_export_queue));

        init();
        my $camps_copy = get_camps();
        my $camps = get_camps();

        set_sync_campaigns($camps, $kind => []);

        my $db_data = get_camps_from_db();
        my $db_specials = get_specials_from_db();
        for my $cid (nsort(keys(%bs_export_queue))) {
            my $test_prefix = get_test_prefix($cid);

            cmp_deeply($camps->{$cid}, $camps_copy->{$cid}, "$test_prefix в хеше \$camps данные по кампании не изменились");
            cmp_deeply($db_data->{$cid}, $bs_export_queue{$cid}, "$test_prefix запись в bs_export_queue не изменилась");

            check_specials(kind => $kind, test_prefix => $test_prefix, cid => $cid, db_specials => $db_specials, expected_cids => {});
        }
    };

    subtest($test_name, $test_code);
}

# тесты 5..7
for my $kind (@all_kinds) {
    my $par_id = 1;
    my $test_name = "[par_id $par_id] вызов set_sync_campaigns [kind = $kind] на кампании, залоченные другим потоком";
    my $test_code = sub {
        plan tests => 3 * scalar(keys(%bs_export_queue));

        init($par_id);
        my @cids = (100 .. 109, 110 .. 119);
        my $camps_copy = get_camps();
        my $camps = get_camps();

        set_sync_campaigns($camps, $kind => \@cids);

        my %exp_cids = map { $_ => undef } @cids;

        my $db_data = get_camps_from_db();
        my $db_specials = get_specials_from_db();

        for my $cid (nsort(keys(%bs_export_queue))) {
            my $test_prefix = get_test_prefix($cid);

            if (exists $exp_cids{$cid}) {
                note('NB! некорректное, но ожидаемое поведение - в перловом коде не проверяется par_id');
                is($camps->{$cid}, undef, "$test_prefix кампания отсутствует в хеше \$camps");
            } else {
                cmp_deeply($camps->{$cid}, $camps_copy->{$cid}, "$test_prefix в хеше \$camps данные по кампании не изменились");
            }

            cmp_deeply($db_data->{$cid}, $bs_export_queue{$cid}, "$test_prefix запись в bs_export_queue не изменилась");

            check_specials(kind => $kind, test_prefix => $test_prefix, cid => $cid, db_specials => $db_specials, expected_cids => \%exp_cids, need_note => 'par_id');
        }
    };

    subtest($test_name, $test_code);
}

# тесты 8..10
for my $kind (@all_kinds) {
    my $par_id = 100;
    my $test_name = "[par_id $par_id] вызов set_sync_campaigns [kind = $kind] на кампании по которым нечего отправлять (send_data = send_price = send_other = 0)";
    my $test_code = sub {
        plan tests => 3 * scalar(keys(%bs_export_queue));

        init($par_id);
        my @cids = (100 .. 109, 110 .. 119);
        my $camps_copy = get_camps();
        my $camps = get_camps();

        set_sync_campaigns($camps, $kind => \@cids);

        my %exp_cids = map { $_ => undef } @cids;

        my $db_data = get_camps_from_db();
        my $db_specials = get_specials_from_db();

        for my $cid (nsort(keys(%bs_export_queue))) {
            my $test_prefix = get_test_prefix($cid);

            if (exists $exp_cids{$cid}) {
                is($camps->{$cid}, undef, "$test_prefix кампания отсутствует в хеше \$camps");
                is($db_data->{$cid}, undef, "$test_prefix кампания удалена из bs_export_queue");
            } else {
                cmp_deeply($camps->{$cid}, $camps_copy->{$cid}, "$test_prefix в хеше \$camps данные по кампании не изменились");
                cmp_deeply($db_data->{$cid}, $bs_export_queue{$cid}, "$test_prefix запись в bs_export_queue не изменилась");
            }

            check_specials(kind => $kind, test_prefix => $test_prefix, cid => $cid, db_specials => $db_specials, expected_cids => \%exp_cids);
        }
    };

    subtest($test_name, $test_code);
}

# тесты 11..13
for my $kind (@all_kinds) {
    my $par_id = 1;
    my $test_name = "[par_id $par_id] вызов set_sync_campaigns [kind = $kind] на кампании по которым есть данные для отправки";
    my $test_code = sub {
        plan tests => 3 * scalar(keys(%bs_export_queue));

        init($par_id);
        my %cids = map { $_ => undef } (1 .. 17, 51 .. 67);
        my $camps_copy = get_camps();
        my $camps = get_camps();

        set_sync_campaigns($camps, $kind => [keys(%cids)]);

        my $pair_kind_flag = $kind2pair_flag{$kind};
        my $flags_to_reset = $kind2flag{$kind};

        my %exp_cids = map {
            $_ => undef
        } grep {
            my $cid = $_;
            !$flags{$cid}->{send_other} && none { $flags{$cid}->{"send_$_"} } @$pair_kind_flag
        } keys(%cids);

        my $db_data = get_camps_from_db();
        my $db_specials = get_specials_from_db();

        for my $cid (nsort(keys(%bs_export_queue))) {
            my $test_prefix = get_test_prefix($cid);

            if (exists $exp_cids{$cid}) {
                # если мы все отправим и нет второго типа данных - удаляем
                is($camps->{$cid}, undef, "$test_prefix кампания отсутствует в хеше \$camps");
                is($db_data->{$cid}, undef, "$test_prefix кампания удалена из bs_export_queue");
            } elsif (exists $cids{$cid}) {
                # если мы позвали функцию на эту кампанию и что-то еще осталось - оставляем залоченной, зануляем статистику и удаляем флаг
                my $queue_copy = dclone($bs_export_queue{$cid});
                my $camp_for_compare = get_camps()->{$cid};

                delete $camp_for_compare-> {"send_$_"} for @$flags_to_reset;
                for my $field (xflatten($kind2fields{$kind})) {
                    $camp_for_compare->{$field} = 0;
                    $queue_copy->{$field} = 0;
                }

                my $fields_str = join(', ', @{ $kind2fields{$kind} });

                my $flags_reset_msg;
                if (@$flags_to_reset > 1) {
                    $flags_reset_msg = "удалены признаки ".join(', ', map { "send_$_" } @$flags_to_reset );
                } else {
                    $flags_reset_msg = "удалён признак send_".$flags_to_reset->[0];
                }
                cmp_deeply($camps->{$cid}, $camp_for_compare, "$test_prefix в хеше \$camps занулена статистика по полям $fields_str; $flags_reset_msg");
                cmp_deeply($db_data->{$cid}, $queue_copy, "$test_prefix в bs_export_queue у кампании занулена статистика по полям $fields_str");
            } else {
                # вообще не должно было поменяться (так как вызывали не на эти номера кампаний)
                cmp_deeply($camps->{$cid}, $camps_copy->{$cid}, "$test_prefix в хеше \$camps данные по кампании не изменились");
                cmp_deeply($db_data->{$cid}, $bs_export_queue{$cid}, "$test_prefix запись в bs_export_queue не изменилась");
            }

            check_specials(kind => $kind, test_prefix => $test_prefix, cid => $cid, db_specials => $db_specials, expected_cids => \%exp_cids);
        }
    };

    subtest($test_name, $test_code);
}

# тесты 14..16
for my $kind (@all_kinds) {
    my $par_id = 100;
    my $test_name = "[par_id $par_id] вызов set_sync_campaigns [kind = $kind] на кампании по которым нечего отправлять (send_data = send_price = send_other = 0), но у которых в базе другое значение sync_val";
    my $test_code = sub {
        plan tests => 3 * scalar(keys(%bs_export_queue));

        init($par_id);
        my @cids = (100 .. 109, 110 .. 119);
        my $camps_copy = get_camps();
        my $camps = get_camps();

        my %exp_cids = map { $_ => undef } @cids;

        for my $camps_hash ($camps, $camps_copy) {
            for my $camp_data (values(%$camps_hash)) {
                next unless exists $exp_cids{$camp_data->{cid}};
                $camp_data->{sync_val} -= 1;
            }
        }

        set_sync_campaigns($camps, $kind => \@cids);


        my $db_data = get_camps_from_db();
        my $db_specials = get_specials_from_db();

        for my $cid (nsort(keys(%bs_export_queue))) {
            my $test_prefix = get_test_prefix($cid);

            if (exists $exp_cids{$cid}) {
                my $queue_copy = dclone($bs_export_queue{$cid});
                $queue_copy->{par_id} = undef;
                $queue_copy->{seq_time} = code(sub {
                    return shift gt $original_seq_time;
                });
                $queue_copy->{queue_time} = code(sub {
                    return shift gt $bs_export_queue{$cid}->{queue_time};
                });

                is($camps->{$cid}, undef, "$test_prefix кампания отсутствует в хеше \$camps");
                cmp_deeply($db_data->{$cid}, $queue_copy, "$test_prefix кампания разлочена в bs_export_queue и переставлена в конец очереди (увеличились queue и seq -time)");
            } else {
                cmp_deeply($camps->{$cid}, $camps_copy->{$cid}, "$test_prefix в хеше \$camps данные по кампании не изменились");
                cmp_deeply($db_data->{$cid}, $bs_export_queue{$cid}, "$test_prefix запись в bs_export_queue не изменилась");
            }

            check_specials(kind => $kind, test_prefix => $test_prefix, cid => $cid, db_specials => $db_specials, expected_cids => \%exp_cids, need_note => 'sync_val');
        }
    };

    subtest($test_name, $test_code);
}

done_testing();
