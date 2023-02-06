#!/usr/bin/perl

use Direct::Modern;

use Test::More;

use Yandex::DBTools;
use Yandex::DBUnitTest qw/:all/;

use BS::ExportMaster;
use Settings;

use constant {
    FIRST_CID => 11,
    TEST_CAMPS_NUM => 7,
    TEST_BORDER => 4,
};


# Создаем фейковый логгер
my $log = bless({}, 'Yandex::FakeLog');
{
    no warnings 'once';
    *Yandex::FakeLog::out = sub {
        # print STDERR @_, "\n";
    };
}

local $BS::ExportMaster::HEAVY_CLIENT_CAMPAIGNS_BORDER = TEST_BORDER;
local $BS::ExportMaster::HEAVY_CLIENT_BANNERS_BORDER = TEST_BORDER;
local $BS::ExportMaster::HEAVY_CLIENT_CONTEXTS_BORDER = TEST_BORDER;
local $BS::ExportMaster::HEAVY_CLIENT_BIDS_BORDER = TEST_BORDER;
BS::ExportMaster::init(shardid => 3);

my @test_cids = (FIRST_CID .. FIRST_CID + TEST_CAMPS_NUM - 1);
my @camps = (
    { cid => 1, uid => 1 },
    map { +{ cid => $_, uid => 20 } } @test_cids,
);

sub get_specials {
    return get_hash_sql(PPC(shard => 3), 'SELECT c.cid, s.par_type FROM campaigns c LEFT JOIN bs_export_specials s USING(cid)');
}

sub check_first_cid {
    is(shift->{1}, undef, 'campaign cid=1 belongs to different user, not moved to heavy');
}

sub init {
    my %tables = @_;
    $tables{bs_export_queue} //= [];
    $tables{bs_export_specials} //= [];

    push @{ $tables{bs_export_queue} }, {
        cid => 1,
        camps_num => 1,
        banners_num => 1,
        contexts_num => 1,
        bids_num => 1,
        prices_num => 1,
    };

    init_test_dataset({
        bs_export_queue => {
            original_db => PPC(shard => 3),
            rows => {
                3 => $tables{bs_export_queue},
            },
        },
        bs_export_specials => {
            original_db => PPC(shard => 3),
            rows => {
                3 => $tables{bs_export_specials},
            },
        },
        campaigns => {
            original_db => PPC(shard => 3),
            rows => {
                3 => \@camps,
            },
        },
    });
}

my @cases = (
    { num => TEST_BORDER - 1, text => 'sum(%s) < border value - not moved to heavy', moved => 0 },
    { num => TEST_BORDER, text => 'sum(%s) == border value - not moved to heavy', moved => 0 },
    { num => TEST_BORDER + 1, text => 'sum(%s) > border value - all camps moved to heavy', moved => 1 },
);

for my $case (@cases) {
    for my $field (qw/ camps_num contexts_num banners_num bids_num /) {
        subtest sprintf($case->{text}, $field) => sub {
            plan tests => 1 + TEST_CAMPS_NUM;
            my @queue;
            for (my $i = 0; $i < TEST_CAMPS_NUM; $i++) {
                my $one_queue_camp = {
                    cid => FIRST_CID + $i,
                    camps_num => 0,
                    banners_num => 0,
                    contexts_num => 0,
                    bids_num => 0,
                    prices_num => 0,
                };
                if ($i < $case->{num}) {
                    $one_queue_camp->{$field}++;
                }
                push @queue, $one_queue_camp;
            }

            init(bs_export_queue => \@queue);
            BS::ExportMaster::move_heavy_campaigns($log);

            my $spec = get_specials();

            check_first_cid($spec);
            for my $cid (@test_cids) {
                is($spec->{$cid},
                   ($case->{moved} ? 'heavy' : undef),
                   "campaign cid=$cid " . ($case->{moved} ? 'moved' : 'not moved') . ' to heavy'
                   );
            }
        };
    }
}

for my $spec_par_type (qw/ fast dev1 dev2 nosend buggy /) {
    subtest "sum(camps_num) > border value - camps moved to heavy, except having par_type=$spec_par_type" => sub {
        plan tests => 1 + TEST_CAMPS_NUM;

        my @queue = map {
            +{
                cid => $_,
                camps_num => 1,
                banners_num => 0,
                contexts_num => 0,
                bids_num => 0,
                prices_num => 0,
            },
        } @test_cids;

        my @specials = (
            { cid => FIRST_CID, par_type => $spec_par_type },
            { cid => FIRST_CID + TEST_CAMPS_NUM - 1, par_type => $spec_par_type },
        );

        init(bs_export_queue => \@queue, bs_export_specials => \@specials);
        BS::ExportMaster::move_heavy_campaigns($log);

        my $spec = get_specials();

        check_first_cid($spec);
        for my $cid (@test_cids) {
            if ($cid == FIRST_CID || $cid == FIRST_CID + TEST_CAMPS_NUM - 1) {
                is($spec->{$cid}, $spec_par_type, "campaign cid=$cid has old par_type $spec_par_type, not moved to heavy");
            } else {
                is($spec->{$cid}, 'heavy', "campaign cid=$cid moved to heavy");
            }
        }
    };
}

subtest "sum(camps_num) > border value - only_cids camps moved to heavy" => sub {
    plan tests => 1 + TEST_CAMPS_NUM;

    my (@queue, %only_cids);
    for (my $i = 0; $i < TEST_CAMPS_NUM; $i++) {
        my $one_queue_camp = {
            cid => FIRST_CID + $i,
            camps_num => 1,
            banners_num => 0,
            contexts_num => 0,
            bids_num => 0,
            prices_num => 0,
        };
        if ($i < TEST_BORDER + 1) {
            $only_cids{ FIRST_CID + $i } = undef;
        }
        push @queue, $one_queue_camp;
    }

    init(bs_export_queue => \@queue);
    BS::ExportMaster::move_heavy_campaigns($log, [keys %only_cids]);

    my $spec = get_specials();
    check_first_cid($spec);
    for my $cid (@test_cids) {
        if (exists $only_cids{$cid}) {
            is($spec->{$cid}, 'heavy', "campaign cid=$cid from only_cids moved to heavy");
        } else {
            is($spec->{$cid}, undef, "campaign cid=$cid not moved to heavy");
        }
    }
};

done_testing();
