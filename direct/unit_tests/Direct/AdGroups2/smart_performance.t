#!/usr/bin/perl

use Direct::Modern;
use open ':std' => ':utf8';

use Settings;
use Direct::Banners;
use Direct::Test::DBObjects;
use Test::More;
use Test::Exception;
use Test::Deep;
use Test::Subtest;
use Tools;
use LogTools;
use Yandex::DBTools;
use Yandex::Clone qw/yclone/;
use Test::JavaIntapiMocks::BidModifiers ':forward_to_perl';
use Test::JavaIntapiMocks::GenerateObjectIds;

no warnings 'redefine';

use_ok('Direct::AdGroups2::Smart');

my $test_data = Direct::Test::DBObjects->new(shard => 1);
$test_data->create_tables();
$test_data->with_user();

undef &LogTools::log_hierarchical_multiplier;
*LogTools::log_hierarchical_multiplier = sub {};

undef &Direct::Banners::do_logging;
*Direct::Banners::do_logging = sub {};

undef &Tools::log_cmd;
*Tools::log_cmd = sub {};

*Client::ClientFeatures::has_access_to_new_feature_from_java = sub { return 0 };

sub test_smart
{
    my ($name, $code) = @_;

    my $group = $test_data->create_adgroup('performance');
    my $banner = $test_data->create_banner('performance', {adgroup_id => $group->id});
    $group->banners([$banner]);

    my $user = $test_data->user;
    my $cid = $group->campaign_id;
    my $group_orig = $group->to_hash;
    my $bid = $group->banners->[0]->id;

    $LogTools::context{uid} = $user->id;
    $group_orig->{uid} = $user->id;
    $group_orig->{adgroup_id} = $group_orig->{id};
    $group_orig->{banners}[0]{banner_id} = $group_orig->{banners}[0]{id};

    subtest $name => sub {
        $code->($group_orig, $user, $cid, $bid);
    };
}

sub _create_apply
{
    my $group_orig = shift;
    unless ($group_orig->{uid}) {
        die "uid missing";
    }
    my $group = yclone($group_orig);
    my $bid = $group->{banners}->[0]->{bid};
    do_update_table(PPC(bid => $bid), 'banners', { statusBsSynced => 'Yes' }, where => { bid => $bid });
    my $smart = Direct::AdGroups2::Smart->from_user_data($group->{uid}, $group->{campaign_id}, [$group]);
    if (!$smart->is_valid) {
        ok(0, "smart invalid: ".($smart->validation_result->get_first_error_description // $smart->errors->[0]));
        return;
    }
    ok(1, 'smart valid');
    lives_ok { $smart->apply() } 'apply successfull';
}

subtest_ 'hierarchical multipliers' => sub {
    test_smart 'performance: add many multipliers' => sub {
        my ($group, $user, $cid, $bid) = @_;

        $group->{hierarchical_multipliers} = {
            mobile_multiplier => {multiplier_pct => 150},
            performance_tgo_multiplier => {multiplier_pct => 50},
        };
        _create_apply($group);

        my $db_data = get_hash_sql(PPC(cid => $cid), "SELECT type, multiplier_pct FROM hierarchical_multipliers WHERE cid = ? AND pid = ?", $cid, $group->{adgroup_id});
        is_deeply($db_data, {
            mobile_multiplier => 150,
            performance_tgo_multiplier => 50,
        }, 'group mobile multiplier saved');
    };

    test_smart 'performance: delete many multipliers' => sub {
        my ($group, $user, $cid, $bid) = @_;

        $group->{hierarchical_multipliers} = {
            mobile_multiplier => {multiplier_pct => 150},
            performance_tgo_multiplier => {multiplier_pct => 50},
        };
        _create_apply($group);
        my $db_data = get_hash_sql(PPC(cid => $cid), "SELECT type, multiplier_pct FROM hierarchical_multipliers WHERE cid = ? AND pid = ?", $cid, $group->{adgroup_id});
        is_deeply($db_data, {
            mobile_multiplier => 150,
            performance_tgo_multiplier => 50,
        }, 'group multipliers saved');

        $group->{hierarchical_multipliers} = {};
        _create_apply($group);

        $db_data = get_hash_sql(PPC(cid => $cid), "SELECT type, multiplier_pct FROM hierarchical_multipliers WHERE cid = ? AND pid = ?", $cid, $group->{adgroup_id});
        is_deeply($db_data, {}, 'group multipliers deleted');
    }
};

run_subtests();
