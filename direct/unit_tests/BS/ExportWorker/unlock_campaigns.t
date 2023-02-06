#!/usr/bin/perl

use Direct::Modern;

use Storable qw/dclone/;
use Test::Deep;
use Test::Exception;
use Test::More;

use Yandex::DBTools;
use Yandex::DBUnitTest qw/:all/;
use Yandex::HashUtils;
use Yandex::ListUtils;
use Yandex::Test::UTF8Builder;

use BS::ExportWorker qw/unlock_campaigns/;
use Settings;

# вместо Yandex::Test::UTF8Builder, так как он не работает для под-тестов
use open ':std' => ':utf8';

# Создаем фейковый логгер
my $log = bless({}, 'Yandex::FakeLog');
{
    no warnings 'once';
    *Yandex::FakeLog::out = sub {
        # print STDERR @_, "\n";
    };
}

my @all_kinds = qw/all camps_and_data camps_only/;
my @fields = qw/cid par_id queue_time seq_time camps_num banners_num contexts_num bids_num prices_num sync_val is_full_export/;
my $original_seq_time = get_one_field_sql(UT, "SELECT NOW() - INTERVAL $BS::ExportWorker::DELAY_TIME - INTERVAL 1 SECOND");
my $original_delay_time = $BS::ExportWorker::DELAY_TIME;

my %bs_export_queue = (
    100 => {
        cid => 100,
        par_id => 100,
        queue_time => '2014-12-31 23:59:59',
        seq_time => $original_seq_time,
        camps_num => 1,
        banners_num => 0,
        contexts_num => 0,
        bids_num => 0,
        prices_num => 0,
        sync_val => 3,
        is_full_export => 0,
    },
    ( map {
        $_ => +{
            cid => $_,
            par_id => 1,
            queue_time => '2015-01-01 00:00:00',
            seq_time => $original_seq_time,
            camps_num => 1,
            banners_num => 3,
            contexts_num => 2,
            bids_num => 4,
            prices_num => 5,
            sync_val => 1,
            is_full_export => 0,
        },
    } (1 .. 17) ),
    110 => {
        cid => 110,
        par_id => 100,
        queue_time => '2014-12-31 23:59:59',
        seq_time => $original_seq_time,
        camps_num => 1,
        banners_num => 0,
        contexts_num => 0,
        bids_num => 0,
        prices_num => 0,
        sync_val => 3,
        is_full_export => 1,
    },
    ( map {
        $_ => +{
            cid => $_,
            par_id => 1,
            queue_time => '2015-01-01 00:00:00',
            seq_time => $original_seq_time,
            camps_num => 1,
            banners_num => 3,
            contexts_num => 2,
            bids_num => 4,
            prices_num => 5,
            sync_val => 1,
            is_full_export => 1,
        },
    } (51 .. 67) ),
);

my %db = (
    bs_export_queue => {
        original_db => PPC(shard => 2),
        rows => {
            2 => [ values %bs_export_queue ],
        },
    },
);

my %flags = (
    1   => { send_camp => 0, send_data => 0, send_other => 0, send_full => 0 },
    2   => { send_camp => 0, send_data => 0, send_other => 1, send_full => 0 },
    3   => { send_camp => 0, send_data => 0, send_other => 0, send_full => 0 },
    4   => { send_camp => 0, send_data => 0, send_other => 1, send_full => 0 },
    5   => { send_camp => 0, send_data => 1, send_other => 0, send_full => 0 },
    6   => { send_camp => 0, send_data => 1, send_other => 1, send_full => 0 },
    7   => { send_camp => 0, send_data => 1, send_other => 0, send_full => 0 },
    8   => { send_camp => 0, send_data => 1, send_other => 1, send_full => 0 },
    9   => { send_camp => 1, send_data => 0, send_other => 0, send_full => 0 },
    10  => { send_camp => 1, send_data => 0, send_other => 1, send_full => 0 },
    11  => { send_camp => 1, send_data => 0, send_other => 0, send_full => 0 },
    12  => { send_camp => 1, send_data => 0, send_other => 1, send_full => 0 },
    13  => { send_camp => 1, send_data => 1, send_other => 0, send_full => 0 },
    14  => { send_camp => 1, send_data => 1, send_other => 1, send_full => 0 },
    15  => { send_camp => 1, send_data => 1, send_other => 0, send_full => 0 },
    16  => { send_camp => 1, send_data => 1, send_other => 1, send_full => 0 },
    17  => { send_camp => 0, send_data => 0, send_other => 0, send_full => 0 },
    100 => { send_camp => 0, send_data => 0, send_other => 0, send_full => 0 },

    51 => { send_camp => 0, send_data => 0, send_other => 0, send_full => 1 },
    52 => { send_camp => 0, send_data => 0, send_other => 1, send_full => 1 },
    53 => { send_camp => 0, send_data => 0, send_other => 0, send_full => 1 },
    54 => { send_camp => 0, send_data => 0, send_other => 1, send_full => 1 },
    55 => { send_camp => 0, send_data => 1, send_other => 0, send_full => 1 },
    56 => { send_camp => 0, send_data => 1, send_other => 1, send_full => 1 },
    57 => { send_camp => 0, send_data => 1, send_other => 0, send_full => 1 },
    58 => { send_camp => 0, send_data => 1, send_other => 1, send_full => 1 },
    59 => { send_camp => 1, send_data => 0, send_other => 0, send_full => 1 },
    60 => { send_camp => 1, send_data => 0, send_other => 1, send_full => 1 },
    61 => { send_camp => 1, send_data => 0, send_other => 0, send_full => 1 },
    62 => { send_camp => 1, send_data => 0, send_other => 1, send_full => 1 },
    63 => { send_camp => 1, send_data => 1, send_other => 0, send_full => 1 },
    64 => { send_camp => 1, send_data => 1, send_other => 1, send_full => 1 },
    65 => { send_camp => 1, send_data => 1, send_other => 0, send_full => 1 },
    66 => { send_camp => 1, send_data => 1, send_other => 1, send_full => 1 },
    67 => { send_camp => 0, send_data => 0, send_other => 0, send_full => 1 },
    110 => { send_camp => 0, send_data => 0, send_other => 0, send_full => 1 },
);

sub get_test_prefix {
    my $cid = shift;
    return sprintf('[cid %3d, par_id %4s; camp %d / data %d / other %d / full_export %d]',
                   $cid,
                   $bs_export_queue{$cid}->{par_id} // 'NULL',
                   @{ $flags{$cid} }{qw/send_camp send_data send_other send_full/},
                   );
}

sub init {
    BS::ExportWorker::init(
        dbh => get_dbh(PPC(shard => 2)),
        dbh2 => get_dbh(PPC(shard => 2)),
        parid => 1,
        partype => 'fake',
        log => $log,
    );
    init_test_dataset(\%db);
}

sub get_camps {
    my %camps;
    for my $camp (@{ $db{bs_export_queue}->{rows}->{2} }) {
        $camps{ $camp->{cid} } = dclone($camp);
        hash_merge($camps{ $camp->{cid} }, $flags{$camp->{cid}});
    }
    return \%camps;
}

sub get_camps_from_db {
    my $fields = join ',', map { sql_quote_identifier($_) } @fields;
    return get_hashes_hash_sql(PPC(shard => 2), "SELECT $fields FROM bs_export_queue");
}

sub check {
    my ($camps, $expected_arr, $delay_arr) = @_;

    my %exp_cids = map { $_ => undef } @$expected_arr;
    my %delay_cids = map { $_ => undef } @{ $delay_arr // [] };

    my $camps_copy = get_camps();
    my $db_camps = get_camps_from_db();
    my $now = get_one_field_sql(UT, 'SELECT NOW()');

    for my $cid (nsort(keys(%bs_export_queue))) {
        my $test_prefix = get_test_prefix($cid);
        my $queue_copy = dclone($bs_export_queue{$cid});

        $camps_copy->{$cid}->{par_id} = ignore();
        $queue_copy->{par_id} = ignore();

        if (%delay_cids) {
            $camps_copy->{$cid}->{seq_time} = ignore();
            $queue_copy->{seq_time} = ignore();
        }

        if (exists $exp_cids{$cid}) {
            $queue_copy->{par_id} = undef;

            is($camps->{$cid}, undef, "$test_prefix кампания отсутствует в хеше \$camps");
            is($db_camps->{$cid}->{par_id}, undef, "$test_prefix запись в bs_export_queue разблокирована (par_id = NULL)")
        } else {
            cmp_deeply($camps->{$cid}, $camps_copy->{$cid}, "$test_prefix данные по кампании в хеше \$camps не изменились");
            is($db_camps->{$cid}->{par_id}, $bs_export_queue{$cid}->{par_id}, "$test_prefix запись в bs_export_queue осталась заблокирована тем же par_id");
        }

        if (exists $delay_cids{$cid}) {
            cmp_ok($db_camps->{$cid}->{seq_time}, 'ge', $original_seq_time, "$test_prefix seq_time в bs_export_queue увеличился");
        } else {
            is($db_camps->{$cid}->{seq_time}, $original_seq_time, "$test_prefix seq_time в bs_export_queue не изменился");
        }

        cmp_ok($db_camps->{$cid}->{seq_time}, 'le', $now, "$test_prefix seq_time в bs_export_queue не превышает NOW()");

        cmp_deeply($db_camps->{$cid}, $queue_copy, "$test_prefix остальные поля в bs_export_queue не изменились");
    }
}

my $COMMON_TESTS_COUNT = 5 * scalar(keys(%bs_export_queue));

# тест 1
dies_ok { unlock_campaigns({}, fake => []) } 'вызов unlock_campaigns с неизвестным kind - умирает';

# тесты 2..4
for my $kind (@all_kinds) {
    my $test_name = "вызов unlock_campaigns [kind = $kind] на кампанию, залоченную другим потоком";
    my $test_code = sub {
        plan tests => 2 * scalar(keys(%bs_export_queue));

        init();
        my $camps_copy = get_camps();
        my $camps = get_camps();

        unlock_campaigns($camps, $kind => [100]);

        my $db_camps = get_camps_from_db();

        for my $cid (nsort(keys(%bs_export_queue))) {
            my $test_prefix = get_test_prefix($cid);

            if ($cid == 100) {
                note('NB! некорректное, но ожидаемое поведение - в перловом коде не проверяется par_id');
                is($camps->{$cid}, undef, "$test_prefix кампания отсутствует в хеше \$camps");
            } else {
                cmp_deeply($camps->{$cid}, $camps_copy->{$cid}, "$test_prefix данные по кампании в хеше \$camps не изменились");
            }

            cmp_deeply($db_camps->{$cid}, $bs_export_queue{$cid}, "$test_prefix запись в bs_export_queue не изменилась");
        }
    };

    subtest($test_name, $test_code);
}

my @tests = (
    # 5
    ( map { +{
        # имя теста
        name => "вызов unlock_campaigns [kind = $_] с пустым списком кампаний",
        # что разблокируем
        kind => $_,
        # какие кампании разблокируем
        cids => [],
        # какие должны разблокироваться
        exp_cids => [],
        # какие хотим подвинуть в очереди
        delay_cids => [],
    } } @all_kinds ),
    {
        name => 'вызов unlock_campaigns [kind = all] на список кампаний',
        kind => 'all',
        cids => [1 .. 17],
        exp_cids => [1 .. 17],
        delay_cids => [],
    },
    {
        name => 'вызов unlock_campaigns [kind = camps_and_data] на список кампаний',
        kind => 'camps_and_data',
        cids => [1 .. 17],
        # все кампании будут удалены (так как ставки мы не учитываем)
        exp_cids => [1 .. 17],
        delay_cids => [],
    },
    {
        name => 'вызов unlock_campaigns [kind = camps_only] на список кампаний',
        kind => 'camps_only',
        cids => [1 .. 17],
        # эти кампании будут удалены из $camps, так как у них нет send_data
        exp_cids => [1 .. 4, 9 .. 12, 17],
        delay_cids => [],
    },
    {
        name => 'вызов unlock_campaigns [kind = all] на список кампаний, часть из которых в %DELAY_CIDS',
        kind => 'all',
        cids => [1 .. 17],
        exp_cids => [1 .. 17],
        delay_cids => [3, 4, 5, 6],
    },
    {
        name => 'вызов unlock_campaigns [kind = camps_and_data] на список кампаний, часть из которых в %DELAY_CIDS',
        kind => 'camps_and_data',
        cids => [1 .. 17],
        # эти кампании будут удалены из $camps (так как ставки мы больше не учитываем)
        exp_cids => [1 .. 17],
        delay_cids => [3, 4, 5, 6],
    },
    {
        name => 'вызов unlock_campaigns [kind = camps_only] на список кампаний, часть из которых в %DELAY_CIDS',
        kind => 'camps_only',
        cids => [1 .. 17],
        # эти кампании будут удалены из $camps, так как у них нет send_data
        exp_cids => [1 .. 4, 9 .. 12, 17],
        delay_cids => [3, 4, 5, 6],
    },
    {
        name => 'вызов unlock_campaigns [kind = all] на список кампаний, часть из которых в %DELAY_CIDS; с таким значением $DELAY_TIME, чтобы seq_time был в будущем',
        kind => 'all',
        cids => [1 .. 17],
        exp_cids => [1 .. 17],
        delay_cids => [3, 4, 5, 6],
        delay_time => '1 MONTH',
    },
);

for my $test (@tests) {
    subtest($test->{name}, sub {
                plan tests => $COMMON_TESTS_COUNT;

                my $camps = get_camps();

                init();
                local $BS::ExportWorker::DELAY_TIME = $test->{delay_time} // $original_delay_time;
                BS::Export::delay_cid($_) for @{ $test->{delay_cids} };
                unlock_campaigns($camps, $test->{kind} => $test->{cids});

                check($camps, $test->{exp_cids}, $test->{delay_cids});
            });
}

done_testing();
