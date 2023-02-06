#!/usr/bin/perl

use warnings;
use strict;
use utf8;

use Test::More;
use Test::Deep;

use Yandex::DBTools;
use Yandex::DBUnitTest qw/:all/;
use Yandex::Test::UTF8Builder;

use my_inc '../../../';
use Settings;

use lib my_inc::path('..');
require TestData;

BEGIN {
    use_ok( 'BS::ExportMaster' );
}

local $Yandex::DBUnitTest::DEFAULT_ENGINE = 'MyISAM';

# не очень надежный тест - так как легко ошибиться в проверночных данных. хорошо бы указывать тип баннера
# и чему скинуть статус и сверять одинаково статистику на 0/не ноль

note('все объекты - без БКшных ID, statusBsSynced = "Yes", если в описании теста не указано иное');
my @tests = (
    {
        name => 'nothing - все данные синхронны',
        prepare_sql => [],
        result => undef,
    },
    {
        name => 'camps - c.statusBsSynced = "No"',
        prepare_sql => ['UPDATE campaigns SET statusBsSynced = "No"'],
        result => {
            camps_num => 1,
            contexts_num => 0,
            banners_num => 0,
            bids_num => 0,
            prices_num => 0,
        },
    },
    {
        name => 'nothing - b.statusBsSynced = "No" (текстовый баннер)',
        prepare_sql => ['UPDATE banners SET statusBsSynced = "No" WHERE bid = 876765397'],
        result => {
            contexts_num => 0,
            banners_num => 1,
            camps_num => 0,
            bids_num => 0,
            prices_num => 0,
      }
    },
    {
        name => 'nothing - p.statusBsSynced = "No" (текстовый баннер)',
        prepare_sql => ['UPDATE phrases SET statusBsSynced = "No" WHERE pid = 686851132'],
        result => {
            camps_num => 0,
            banners_num => 0,
            prices_num => 0,
            bids_num => 5,
            contexts_num => 2
        }
    },
    {
        # NB! кажется статистика считается неправильно. Новый баннер в существующий контекст отправляется вместе с контекстом
        name => 'banners - b.statusBsSynced = "No", p.PriorityID = 1 (текстовый баннер)',
        prepare_sql => [
            'UPDATE banners SET statusBsSynced = "No" WHERE bid = 876765397',
            'UPDATE phrases SET PriorityID = 1 WHERE pid = 686851132',
        ],
        result => {
            camps_num => 0,
            contexts_num => 0,
            banners_num => 1,
            bids_num => 0,
            prices_num => 0,
        },
    },
    {
        name => 'banners, contexts, bids - b.statusBsSynced = "No", p.statusBsSynced = "No" (текстовый баннер)',
        prepare_sql => [
            'UPDATE banners SET statusBsSynced = "No" WHERE bid = 876765397',
            'UPDATE phrases SET statusBsSynced = "No" WHERE pid = 686851132',
        ],
        result => {
            contexts_num => 2,
            camps_num => 0,
            prices_num => 0,
            banners_num => 1,
            bids_num => 5
        },
    },
    {
        name => 'contexts, bids - p.statusBsSynced = "No", b.BannerID = 1 (текстовый баннер)',
        prepare_sql => [
            'UPDATE phrases SET statusBsSynced = "No" WHERE pid = 686851132',
            'UPDATE banners SET BannerID = 1 WHERE bid = 876765397',
        ],
        result => {
            camps_num => 0,
            contexts_num => 2,
            banners_num => 0,
            bids_num => 5,
            prices_num => 0,
        },
    },
    {
        name => 'contexts, banners, bids - p.statusBsSynced = "No", b.statusBsSynced = "No" (текстовый баннер)',
        prepare_sql => [
            'UPDATE phrases SET statusBsSynced = "No" WHERE pid = 686851132',
            'UPDATE banners SET statusBsSynced = "No" WHERE bid = 876765397',
        ],
        result => {
            camps_num => 0,
            contexts_num => 2,
            banners_num => 1,
            bids_num => 5,
            prices_num => 0,
        },
    },
    {
        name => 'nothing - bi.statusBsSynced = "No" (текстовый баннер)',
        prepare_sql => [
            'UPDATE bids SET statusBsSynced = "No" WHERE pid = 686851132',
        ],
        result => undef,
    },
    {
        name => 'nothing - bi.statusBsSynced = "No", p.PriorityID = 1 (текстовый баннер)',
        prepare_sql => [
            'UPDATE bids SET statusBsSynced = "No" WHERE pid = 686851132',
            'UPDATE phrases SET PriorityID = 1 WHERE pid = 686851132',
        ],
        result => undef,
    },
    {
        name => 'nothing - bi.statusBsSynced = "No", p.PriorityID = 1, b.BannerID = 1 (текстовый баннер)',
        prepare_sql => [
            'UPDATE bids SET statusBsSynced = "No" WHERE pid = 686851132',
            'UPDATE phrases SET PriorityID = 1 WHERE pid = 686851132',
            'UPDATE banners SET BannerID = 1 WHERE bid = 876765397',
        ],
        result => undef,
    },
    {
        name => 'prices - bi.statusBsSynced = "No", bi.PhraseID = 1, p.PriorityID = 1, b.BannerID = 1 (текстовый баннер)',
        prepare_sql => [
            'UPDATE bids SET statusBsSynced = "No", PhraseID = 1 WHERE pid = 686851132',
            'UPDATE phrases SET PriorityID = 1 WHERE pid = 686851132',
            'UPDATE banners SET BannerID = 1 WHERE bid = 876765397',
        ],
        result => undef,
    },
    {
        name => 'nothing - bi_ret.statusBsSynced = "No" (текстовый баннер)',
        prepare_sql => [
            'UPDATE bids_retargeting SET statusBsSynced = "No" WHERE pid = 686851132',
        ],
        result => undef,
    },
    {
        name => 'nothing - bi_ret.statusBsSynced = "No", p.PriorityID = 1 (текстовый баннер)',
        prepare_sql => [
            'UPDATE bids_retargeting SET statusBsSynced = "No" WHERE pid = 686851132',
            'UPDATE phrases SET PriorityID = 1 WHERE pid = 686851132',
        ],
        result => undef,
    },
    {
        name => 'nothing - bi_ret.statusBsSynced = "No", p.statusBsSynced = "No" (текстовый баннер)',
        prepare_sql => [
            'UPDATE bids_retargeting SET statusBsSynced = "No" WHERE pid = 686851132',
            'UPDATE phrases SET statusBsSynced = "No" WHERE pid = 686851132',
        ],
        result => {
            banners_num => 0,
            camps_num => 0,
            contexts_num => 2,
            prices_num => 0,
            bids_num => 5
        },
    },
    {
        name => 'contexts, bids - bi_ret.statusBsSynced = "No", p.statusBsSynced = "No", b.BannerID = 1 (текстовый баннер)',
        prepare_sql => [
            'UPDATE bids_retargeting SET statusBsSynced = "No" WHERE pid = 686851132',
            'UPDATE phrases SET statusBsSynced = "No" WHERE pid = 686851132',
            'UPDATE banners SET BannerID = 1 WHERE bid = 876765397',
        ],
        result => {
            camps_num => 0,
            contexts_num => 2,
            banners_num => 0,
            bids_num => 5,
            prices_num => 0,
        },
    },
    {
        name => 'nothing - bi_ret.statusBsSynced = "No", b.BannerID = 1 (текстовый баннер)',
        prepare_sql => [
            'UPDATE bids_retargeting SET statusBsSynced = "No" WHERE pid = 686851132',
            'UPDATE banners SET BannerID = 1 WHERE bid = 876765397',
        ],
        result => undef,
    },
    {
        name => 'prices - bi_ret.statusBsSynced = "No", p.PriorityID = 1, b.BannerID = 1 (текстовый баннер)',
        prepare_sql => [
            'UPDATE bids_retargeting SET statusBsSynced = "No" WHERE pid = 686851132',
            'UPDATE phrases SET PriorityID = 1 WHERE pid = 686851132',
            'UPDATE banners SET BannerID = 1 WHERE bid = 876765397',
        ],
        result => undef,
    },
    {
        # NB! кажется статистика считается неправильно. Новый баннер в существующий контекст отправляется вместе с контекстом
        name => 'banners - b.statusBsSynced = "No", p.PriorityID = 1 (текстовый баннер с картинкой)',
        prepare_sql => [
            'UPDATE banners SET statusBsSynced = "No" WHERE bid = 876765407',
            'UPDATE phrases SET PriorityID = 1 WHERE pid = 686851137',
        ],
        result => {
            camps_num => 0,
            contexts_num => 0,
            banners_num => 1,
            bids_num => 0,
            prices_num => 0,
        },
    },
    {
        name => 'banners, contexts, bids - b.statusBsSynced = "No", p.statusBsSynced = "No" (текстовый баннер с картинкой)',
        prepare_sql => [
            'UPDATE banners SET statusBsSynced = "No" WHERE bid = 876765407',
            'UPDATE phrases SET statusBsSynced = "No" WHERE pid = 686851137',
        ],
        result => {
            camps_num => 0,
            contexts_num => 1,
            banners_num => 1,
            bids_num => 4,
            prices_num => 0,
        },
    },
    {
        name => 'contexts, bids - p.statusBsSynced = "No", b.BannerID = 1 (текстовый баннер с картинкой)',
        prepare_sql => [
            'UPDATE phrases SET statusBsSynced = "No" WHERE pid = 686851137',
            'UPDATE banners SET BannerID = 1 WHERE bid = 876765407',
        ],
        result => {
            camps_num => 0,
            contexts_num => 1,
            banners_num => 0,
            bids_num => 4,
            prices_num => 0,
        },
    },
    {
        name => 'contexts, banners, bids - p.statusBsSynced = "No", b.statusBsSynced = "No" (текстовый баннер с картинкой)',
        prepare_sql => [
            'UPDATE phrases SET statusBsSynced = "No" WHERE pid = 686851137',
            'UPDATE banners SET statusBsSynced = "No" WHERE bid = 876765407',
        ],
        result => {
            camps_num => 0,
            contexts_num => 1,
            banners_num => 1,
            bids_num => 4,
            prices_num => 0,
        },
    },
    {
        name => 'nothing - bi.statusBsSynced = "No" (текстовый баннер с картинкой)',
        prepare_sql => [
            'UPDATE bids SET statusBsSynced = "No" WHERE pid = 686851137',
        ],
        result => undef,
    },
    {
        name => 'nothing - bi.statusBsSynced = "No", p.PriorityID = 1 (текстовый баннер с картинкой)',
        prepare_sql => [
            'UPDATE bids SET statusBsSynced = "No" WHERE pid = 686851137',
            'UPDATE phrases SET PriorityID = 1 WHERE pid = 686851137',
        ],
        result => undef,
    },
    {
        name => 'nothing - bi.statusBsSynced = "No", p.PriorityID = 1, b.BannerID = 1 (текстовый баннер с картинкой)',
        prepare_sql => [
            'UPDATE bids SET statusBsSynced = "No" WHERE pid = 686851137',
            'UPDATE phrases SET PriorityID = 1 WHERE pid = 686851137',
            'UPDATE banners SET BannerID = 1 WHERE bid = 876765407',
        ],
        result => undef,
    },
    {
        name => 'prices - bi.statusBsSynced = "No", bi.PhraseID = 1, p.PriorityID = 1, b.BannerID = 1 (текстовый баннер с картинкой)',
        prepare_sql => [
            'UPDATE bids SET statusBsSynced = "No", PhraseID = 1 WHERE pid = 686851137',
            'UPDATE phrases SET PriorityID = 1 WHERE pid = 686851137',
            'UPDATE banners SET BannerID = 1 WHERE bid = 876765407',
        ],
        result => undef,
    },
    {
        name => 'nothing - p.statusBsSynced = "No" (динамический баннер)',
        prepare_sql => ['UPDATE phrases SET statusBsSynced = "No" WHERE pid = 686851142'],
        result => {
            banners_num => 0,
            camps_num => 0,
            prices_num => 0,
            bids_num => 2,
            contexts_num => 4
        },
    },
    {
        name => 'nothing - b.statusBsSynced = "No" (динамический баннер)',
        prepare_sql => ['UPDATE banners SET statusBsSynced = "No" WHERE bid = 876765412'],
        result => {
            camps_num => 0,
            bids_num => 0,
            prices_num => 0,
            banners_num => 1,
            contexts_num => 0
        },
    },
    {
        name => 'contexts, banners, bids - b.statusBsSynced = "No", p.statusBsSynced = "No" (динамический баннер)',
        prepare_sql => [
            'UPDATE banners SET statusBsSynced = "No" WHERE bid = 876765412',
            'UPDATE phrases SET statusBsSynced = "No" WHERE pid = 686851142',
        ],
        result => {
            camps_num => 0,
            contexts_num => 4,
            bids_num => 2,
            prices_num => 0,
            banners_num => 1
        },
    },
    {
        name => 'banners - b.statusBsSynced = "No", p.PriorityID = 1 (динамический баннер)',
        prepare_sql => [
            'UPDATE banners SET statusBsSynced = "No" WHERE bid = 876765412',
            'UPDATE phrases SET PriorityID = 1 WHERE pid = 686851142',
        ],
        result => {
            camps_num => 0,
            contexts_num => 0,
            banners_num => 1,
            bids_num => 0,
            prices_num => 0,
        },
    },
    {
        name => 'contexts, bids - b.BannerID = 1, p.statusBsSynced = "No" (динамический баннер)',
        prepare_sql => [
            'UPDATE banners SET BannerID = 1 WHERE bid = 876765412',
            'UPDATE phrases SET statusBsSynced = "No" WHERE pid = 686851142',
        ],
        result => {
            banners_num => 0,
            bids_num => 2,
            prices_num => 0,
            contexts_num => 4,
            camps_num => 0
        },
    },
    {
        name => 'nothing - bi_dyn.statusBsSynced = "No" (динамический баннер)',
        prepare_sql => ['UPDATE bids_dynamic SET statusBsSynced = "No" WHERE pid = 686851142'],
        result => undef,
    },
    {
        name => 'prices - bi_dyn.statusBsSynced = "No", p.PriorityID = 1 (динамический баннер)',
        prepare_sql => [
            'UPDATE bids_dynamic SET statusBsSynced = "No" WHERE pid = 686851142',
            'UPDATE phrases SET PriorityID = 1 WHERE pid = 686851142',
        ],
        result => undef,
    },
    {
        name => 'nothing - bi_dyn.statusBsSynced = "No", p.statusBsSynced = "No" (динамический баннер)',
        prepare_sql => [
            'UPDATE bids_dynamic SET statusBsSynced = "No" WHERE pid = 686851142',
            'UPDATE phrases SET statusBsSynced = "No" WHERE pid = 686851142',
        ],
        result => {
            contexts_num => 4,
            camps_num => 0,
            prices_num => 0,
            banners_num => 0,
            bids_num => 2
        },
    },
    {
        name => 'contexts, bids - bi_dyn.statusBsSynced = "No", p.statusBsSynced = "No", b.BannerID = 1 (динамический баннер)',
        prepare_sql => [
            'UPDATE bids_dynamic SET statusBsSynced = "No" WHERE pid = 686851142',
            'UPDATE phrases SET statusBsSynced = "No" WHERE pid = 686851142',
            'UPDATE banners SET BannerID = 1 WHERE bid = 876765412',
        ],
        result => {
            camps_num => 0,
            contexts_num => 4,
            banners_num => 0,
            bids_num => 2,
            prices_num => 0,
        },
    },
);

# Создаем фейковый логгер
my $log = {};
bless($log, 'Yandex::FakeLog');
{
    no warnings 'once';
    *Yandex::FakeLog::out = sub {
        note($_[1]);
    };
}

sub check_test_case {
    my $case = shift;
    init_test_dataset($TestData::db);
    BS::ExportMaster::init(shardid => $TestData::TEST_SHARD);
    note('do prepare_sql');
    for my $query (@{ $case->{prepare_sql} }){
        do_sql(SHUT(shard => $TestData::TEST_SHARD), $query);
    }
    my $cid = $TestData::db->{campaigns}->{rows}->{$TestData::TEST_SHARD}->[4]->{cid};
    BS::ExportMaster::update_status_bs_synced($log, [$cid]);
    my $result = get_one_line_sql(SHUT(shard => $TestData::TEST_SHARD), 'SELECT camps_num, contexts_num, banners_num, bids_num, prices_num FROM bs_export_queue WHERE cid = ?', $cid);
    cmp_deeply($result, $case->{result}, $case->{name});
}

for my $case (@tests) {
    check_test_case($case);
}

done_testing();
