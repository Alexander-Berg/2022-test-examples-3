#!/usr/bin/env perl

use Direct::Modern;

use Test::More;

use Settings;
use Direct::Test::DBObjects;

use Test::JavaIntapiMocks::GenerateObjectIds;

BEGIN {
    use_ok('Direct::Keywords');
    use_ok('Direct::AdGroups2');
    use_ok('Direct::Banners');
}

subtest 'Tests with database access' => sub {
    Direct::Test::DBObjects->create_tables;
    my $db_obj_c = Direct::Test::DBObjects->new(shard => 1)->with_campaign('text');
    local $LogTools::context{uid} = $db_obj_c->user->id;

    no warnings 'redefine';
    local *Direct::Keywords::do_logging = sub {};

    subtest 'Prepare to delete all keywords conditions' => sub {
        my $adgroup = $db_obj_c->create_adgroup('base', {status_moderate=>'Yes',
                                                         status_post_moderate => 'Yes',
                                                         status_bs_synced => 'Yes',
        });
        $db_obj_c->create_banner('base', {status_moderate=>'Yes', status_post_moderate => 'Yes',
                                          status_bs_synced => 'Yes', vcard_status_moderate => 'Yes',
                                          adgroup_id=>$adgroup->id});
        my $keyword1 = $db_obj_c->create_keyword({status_moderate=>'Yes', status_bs_synced => 'Yes', adgroup_id=>$adgroup->id});
        my $keyword2 = $db_obj_c->create_keyword({status_moderate=>'Yes', status_bs_synced => 'Yes', adgroup_id=>$adgroup->id});

        my $adgroup_db = Direct::AdGroups2->get_by(adgroup_id =>$adgroup->id, adgroup_type=>['base'], extended=>1);
        $keyword1->adgroup($adgroup_db->items->[0]);
        $keyword2->adgroup($adgroup_db->items->[0]);

        Direct::Keywords->new([$keyword1, $keyword2])->delete();

        my $adgroup_db2 = Direct::AdGroups2->get_by(adgroup_id =>$adgroup->id, adgroup_type=>['base'])->items->[0];
        my $banner_db2 = Direct::Banners->get_by(adgroup_id =>$adgroup->id)->items->[0];

        check_adgroup($adgroup_db2);
        check_banner($_) foreach @{Direct::Banners->get_by(adgroup_id =>$adgroup->id)->items};
    };
    subtest 'Prepare to delete one keyword condition' => sub {
        my $adgroup = $db_obj_c->create_adgroup('base', {status_moderate=>'Yes',
                                                         status_post_moderate => 'Yes',
                                                         status_bs_synced => 'Yes',
        });
        $db_obj_c->create_banner('base', {status_moderate=>'Yes', status_post_moderate => 'Yes',
                                          status_bs_synced => 'Yes', vcard_status_moderate => 'Yes',
                                          adgroup_id=>$adgroup->id});
        my $keyword1 = $db_obj_c->create_keyword({status_moderate=>'Yes', status_bs_synced => 'Yes', adgroup_id=>$adgroup->id});
        my $keyword2 = $db_obj_c->create_keyword({status_moderate=>'Yes', status_bs_synced => 'Yes', adgroup_id=>$adgroup->id});

        my $adgroup_db = Direct::AdGroups2->get_by(adgroup_id =>$adgroup->id, adgroup_type=>['base'], extended=>1);
        $keyword1->adgroup($adgroup_db->items->[0]);
        $keyword2->adgroup($adgroup_db->items->[0]);

        Direct::Keywords->new([$keyword1])->delete();

        my $adgroup_db2 = Direct::AdGroups2->get_by(adgroup_id =>$adgroup->id, adgroup_type=>['base'])->items->[0];
        my $banner_db2 = Direct::Banners->get_by(adgroup_id =>$adgroup->id)->items->[0];

        check_adgroup($adgroup_db2, status_moderate=>'Yes', status_post_moderate => 'Yes');
        check_banner($_, status_moderate=>'Yes', status_post_moderate => 'Yes',
                         vcard_status_moderate => 'Yes', status_bs_synced => 'Yes')
            foreach @{Direct::Banners->get_by(adgroup_id =>$adgroup->id)->items};
    };


};

sub check_adgroup {
    my ($adgroup_model, %overwrite) = @_;
    is_deeply(
        {map {$_ => $adgroup_model->{$_}} qw/status_moderate status_post_moderate status_bs_synced/},
        {status_moderate => $overwrite{status_moderate} || 'Yes',
         status_post_moderate => $overwrite{status_post_moderate} || 'Yes',
         status_bs_synced => $overwrite{status_bs_synced} || 'No'},
        'check_adgroup'
    );
}
sub check_banner {
    my ($banner_model, %overwrite) = @_;
    is_deeply(
            {map {$_ => $banner_model->{$_}} qw/status_moderate status_post_moderate status_bs_synced vcard_status_moderate/},
            {status_moderate => $overwrite{status_moderate} || 'Yes',
             status_post_moderate => $overwrite{status_post_moderate} || 'Yes',
             status_bs_synced => $overwrite{status_bs_synced} || 'Yes',
             vcard_status_moderate => $overwrite{vcard_status_moderate} || 'Yes'},
        'check_banner'
    );
}

done_testing;
