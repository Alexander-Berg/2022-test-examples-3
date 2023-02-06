#!/usr/bin/perl

use Direct::Modern;
use open ':std' => ':utf8';
use Test::More;
use Test::Exception;
use Test::Deep;
use Test::CreateDBObjects;
use Test::Subtest;
use LogTools;
use Settings;
use Yandex::DBTools;
use Yandex::Clone qw/yclone/;
use Lang::Guess;
use Test::JavaIntapiMocks::BidModifiers ':forward_to_perl';
use Test::JavaIntapiMocks::GenerateObjectIds;

no warnings 'redefine';

use_ok('Direct::AdGroups2::Smart');

undef &BS::TrafaretAuction::trafaret_auction;
*BS::TrafaretAuction::trafaret_auction = sub { };

undef &Lang::Guess::analyze_text_lang;
*Lang::Guess::analyze_text_lang = sub { 'ru' };

create_tables();

undef &LogTools::log_hierarchical_multiplier;
*LogTools::log_hierarchical_multiplier = sub {};

*Client::ClientFeatures::has_access_to_new_feature_from_java = sub { return 0 };

undef &BannerStorage::_send_metrics_to_solomon;
*BannerStorage::_send_metrics_to_solomon = sub {};

sub test_smart
{
    my ($name, $code) = @_;
    
    my $user = create('user');
    my $cid = create(campaign => %$user);
    my $group_orig = create(group => (cid => $cid));
    my $bid = $group_orig->{banners}->[0]->{bid};
    
    $LogTools::context{uid} = $group_orig->{uid};

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
    my $smart = Direct::AdGroups2::Smart->from_user_data($group->{uid}, $group->{cid}, [$group]);
    if (!$smart->is_valid) {
        ok(0, "smart invalid: ".($smart->validation_result->get_first_error_description // $smart->errors->[0]));
        return;
    }
    ok(1, 'smart valid');
    lives_ok { $smart->apply() } 'apply successfull';
}

subtest_ 'video_additions' => sub {
    do_insert_into_table(PPCDICT, 'media_resources', {
            media_resource_id => 1,
            resource_type => 'video',
            name => 'test',
            colors => '',
            preview_url => '',
            resources_url => '' 
    });

    test_smart 'create_apply' => sub {
        my ($group, undef, $cid) = @_;
        _create_apply($group);
    };

    test_smart 'no_changes' => sub {
        my ($group, undef, $cid, $bid) = @_;
        do_update_table(PPC(bid => $bid), 'banners',   { statusModerate => 'Yes' }, where => { bid => $bid });
        do_update_table(PPC(cid => $cid), 'campaigns', { statusModerate => 'Yes' }, where => { cid => $cid });
       _create_apply($group);
       is(get_one_field_sql(PPC(bid => $bid), "select statusBsSynced from banners where bid = ?", $bid), 'Yes', 'statusBsSynced not changed');
    };
    
    test_smart 'add_video' => sub {
        my ($group, $user, $cid, $bid) = @_;
        $group->{banners}->[0]->{statusModerate} = 'Yes';
        do_update_table(PPC(bid => $bid), 'banners',   { statusModerate => 'Yes' }, where => { bid => $bid });
        do_update_table(PPC(cid => $cid), 'campaigns', { statusModerate => 'Yes' }, where => { cid => $cid });
        _create_apply($group);

        my $creative_id = Test::CreateDBObjects::create_perf_creative_video($user, $group);
        do_delete_from_table(PPC(cid => $cid), 'banners_performance', where => { creative_id => $creative_id });

        $group->{banners}->[0]->{video_resources} = {
            id => $creative_id,
            name => "тестовое видео дополнение",
            resource_type => "creative",
            resources_url => "https://ya.ru/",
        };
        _create_apply($group);
        my $bp = get_one_line_sql(PPC(uid => $group->{uid}), "select cid, pid, bid, statusModerate from banners_performance where bid = ?", $bid);
        cmp_deeply($bp, { bid => $bid, cid => $cid, pid => $group->{pid}, statusModerate => 'Ready' }, "banners_performance created");
        is(get_one_field_sql(PPC(bid => $bid), "select statusBsSynced from banners where bid = ?", $bid), 'No', "banners statusBsSynced = No");
    };

    test_smart 'replace_video' => sub {
        my ($group, $user, $cid, $bid) = @_;
        my $creative_id1 = Test::CreateDBObjects::create_perf_creative_video($user, $group);
        do_delete_from_table(PPC(cid => $cid), 'banners_performance', where => { creative_id => $creative_id1 });
        my $creative_id2 = Test::CreateDBObjects::create_perf_creative_video($user, $group);

        $group->{banners}->[0]->{video_resources} = {
            id => $creative_id1,
            name => "тестовое видео дополнение",
            resource_type => "creative",
            resources_url => "https://ya.ru/",
        };

        do_update_table(PPC(bid => $bid), banners_performance => { statusModerate => 'No' }, where => { bid => $bid });
        
        $group->{banners}->[0]->{statusModerate} = 'Yes';
        do_update_table(PPC(bid => $bid), 'banners',   { statusModerate => 'Yes' }, where => { bid => $bid });
        do_update_table(PPC(cid => $cid), 'campaigns', { statusModerate => 'Yes' }, where => { cid => $cid });

        _create_apply($group);
        
        my $bp = get_one_line_sql(PPC(uid => $group->{uid}), "select cid, pid, bid, statusModerate, creative_id from banners_performance where bid = ?", $bid);
        cmp_deeply($bp, { bid => $bid, cid => $cid, pid => $group->{pid}, statusModerate => 'Ready', creative_id => $creative_id1 }, "banners_performance created");
        is(get_one_field_sql(PPC(bid => $bid), "select statusBsSynced from banners where bid = ?", $bid), 'No', "banners statusBsSynced = No");

    };

    test_smart 'send_to_moderate_after_replace' => sub {
        my ($group, $user, $cid, $bid) = @_;
        my $banner_resource = {resource_id => 1, bid => $bid, used_resources => '[1]'};
        do_insert_into_table(PPC(bid => $bid), "banner_resources", $banner_resource);
        
        $group->{banners}->[0]->{statusModerate} = 'Yes';
        do_update_table(PPC(bid => $bid), 'banners',   { statusModerate => 'Yes' }, where => { bid => $bid });
        do_update_table(PPC(cid => $cid), 'campaigns', { statusModerate => 'Yes' }, where => { cid => $cid });

        $group->{banners}->[0]->{video_resources} = {
            id => 1,
            name => "тестовое видео дополнение",
            resource_type => "media",
            resources_url => "https://ya.ru/",
        };
        _create_apply($group);
        
        my $creative_id = Test::CreateDBObjects::create_perf_creative_video($user, $group);
        do_delete_from_table(PPC(cid => $cid), 'banners_performance', where => { creative_id => $creative_id });

        $group->{banners}->[0]->{video_resources} = {
            id => $creative_id,
            name => "тестовое видео дополнение",
            resource_type => "creative",
            resources_url => "https://ya.ru/",
        };

        _create_apply($group);
        is(get_one_field_sql(PPC(bid => $bid), 'select statusModerate from banners_performance where bid = ?', $bid), 'Ready',
            'new creative status_moderate = Ready');
    };
    
    test_smart 'send_to_moderate_after_replace2' => sub {
        my ($group, $user, $cid, $bid) = @_;

        $group->{banners}->[0]->{statusModerate} = 'Yes';
        do_update_table(PPC(bid => $bid), 'banners',   { statusModerate => 'Yes' }, where => { bid => $bid });
        do_update_table(PPC(cid => $cid), 'campaigns', { statusModerate => 'Yes' }, where => { cid => $cid });

        my $creative_id = Test::CreateDBObjects::create_perf_creative_video($user, $group);
        do_delete_from_table(PPC(cid => $cid), 'banners_performance', where => { creative_id => $creative_id });

        $group->{banners}->[0]->{video_resources} = {
            id => $creative_id,
            name => "тестовое видео дополнение",
            resource_type => "creative",
            resources_url => "https://ya.ru/",
        };
        _create_apply($group);
        
        do_delete_from_table(PPC(cid => $cid), 'banners_performance', where => { bid => $bid });
        my $creative_id2 = Test::CreateDBObjects::create_perf_creative_video($user, $group);
        do_update_table(PPC(cid => $cid), 'banners_performance', { statusModerate => 'Sent', creative_id => $creative_id }, where => { bid => $bid });
        
        $group = Models::AdGroup::get_groups({cid => $cid, pid => $group->{pid}})->[0];

        $group->{banners}->[0]->{video_resources} = {
            id => $creative_id2,
            name => "тестовое видео дополнение",
            resource_type => "creative",
            resources_url => "https://ya.ru/",
        };

        _create_apply($group);
        is(get_one_field_sql(PPC(bid => $bid), 'select statusModerate from banners_performance where bid = ?', $bid), 'Ready',
            'new creative status_moderate = Ready');
    };
    
    test_smart 'remove_video' => sub {
        my ($group, $user, $cid, $bid) = @_;
        
        my $creative_id = Test::CreateDBObjects::create_perf_creative_video($user, $group);
        do_delete_from_table(PPC(cid => $cid), 'banners_performance', where => { creative_id => $creative_id });

        $group->{banners}->[0]->{video_resources} = {
            id => $creative_id,
            name => "тестовое видео дополнение",
            resource_type => "creative",
            resources_url => "https://ya.ru/",
        };

        _create_apply($group);
        
        $group->{banners}->[0]->{video_resources} = { };
        _create_apply($group);
        my $bp = get_one_line_sql(PPC(uid => $group->{uid}), "select cid, pid, bid, statusModerate, creative_id from banners_performance where bid = ?", $bid);
        is($bp, undef, "banners_performance deleted");
        is(get_one_field_sql(PPC(bid => $bid), "select statusBsSynced from banners where bid = ?", $bid), 'No', "banners statusBsSynced = No");
    };
};

subtest_ 'hierarchical multipliers' => sub {
    test_smart 'text: add mobile multiplier' => sub {
        my ($group, $user, $cid, $bid) = @_;

        $group->{hierarchical_multipliers} = {
            mobile_multiplier => {multiplier_pct => 150},
        };
        _create_apply($group);

        my $db_data = get_hash_sql(PPC(cid => $cid), "SELECT type, multiplier_pct FROM hierarchical_multipliers WHERE cid = ? AND pid = ?", $cid, $group->{pid});
        is($db_data->{mobile_multiplier}, 150, 'group mobile multiplier saved');
    }
};

run_subtests();
