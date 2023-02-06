use Direct::Modern;


use Settings;

use Test::More;
use Yandex::Test::ValidationResult;

use Yandex::DBUnitTest qw/init_test_dataset/;

BEGIN {
    use_ok('Direct::Model::Campaign');
    use_ok('Direct::Validation::Campaigns', qw/validate_copy_campaigns_for_client/);
}

my %db = (
    shard_inc_cid => {
        original_db => PPCDICT,
        rows => []
    },
    banners_performance => {
        original_db => PPC(shard => 1),
        like => 'banners_performance',
        rows => [],
    },
    perf_creatives => {
        original_db => PPC(shard => 1),
        like => 'perf_creatives',
        rows => [],
    }
);

init_test_dataset(\%db);

package Test::Direct::Model::Campaign {
    use Mouse;
    extends 'Direct::Model::Campaign';
    with 'Direct::Model::Campaign::Role::HasBanners', 'Direct::Model::Campaign::Role::CampQueue';
    1;
}

subtest 'validate copy campaigns for client' => sub {
    no warnings 'redefine';
    local *Client::get_clientid = sub {
        return 1;
    };
    local *Client::get_client_campaign_count = sub {
        my %count = (total_count => 1, unarc_count => 1);
        return \%count;
    };
    local *Client::get_client_limits = sub {
        my %limit = (camp_count_limit => 3, unarc_camp_count_limit => 3);
        return \%limit;
    };

    local *Client::ClientFeatures::has_cpm_deals_allowed_feature = sub {
        return 1;
    };

    local *Client::ClientFeatures::has_cpm_yndx_frontpage_allowed_feature = sub {
        return 1;
    };
    
    local *Client::ClientFeatures::has_content_promotion_video_allowed_feature = sub {
        return 1;
    };

    local *PrimitivesIds::get_pids = sub {
        return [1,2];
    };

    local *Models::AdGroup::is_completed_groups = sub {
        my ($pids) = @_;
        $pids //= [];
        return {map {$_ => 1} grep {$_%2} @$pids};
    };

    use warnings 'redefine';

    local *vr = sub {
        my ($campaigns, $client_id) = @_;
        validate_copy_campaigns_for_client(
            get_campaigns(\@$campaigns),
            $client_id,
            {role => 'client'}
        );
    };
    ok_validation_result(
        vr([{id => 1, user_id => 123, manager_user_id => undef, agency_user_id=> undef, campaign_type => 'text', status_archived => 'No',
                client_id => 333, has_banners => 1, is_in_camp_operations_queue => 0, is_in_camp_operations_queue_copy => 0}],
            333));
    cmp_validation_result(
        vr([{id => 1, user_id => 123, manager_user_id => undef, agency_user_id=> undef, campaign_type => 'text', status_archived => 'No',
                client_id => 333, has_banners => 1, is_in_camp_operations_queue => 0, is_in_camp_operations_queue_copy => 0}],
            444),
        { 1 =>  vr_errors('AccessDenied') }
    );
    ok_validation_result(
        vr([{id => 1, user_id => 123, manager_user_id => 1, agency_user_id=> undef, campaign_type => 'text', status_archived => 'No',
                client_id => 333, has_banners => 1, is_in_camp_operations_queue => 0, is_in_camp_operations_queue_copy => 0}],
            333)
    );
    ok_validation_result(
        vr([{id => 1, user_id => 123, manager_user_id => undef, agency_user_id=> 78, campaign_type => 'text', status_archived => 'No',
                client_id => 333, has_banners => 1, is_in_camp_operations_queue => 0, is_in_camp_operations_queue_copy => 0}],
            333)
    );

    cmp_validation_result(
        vr([{id => 1, user_id => 123, manager_user_id => undef, agency_user_id=> undef, campaign_type => 'mcb', status_archived => 'No',
                client_id => 333, has_banners => 1, is_in_camp_operations_queue => 0, is_in_camp_operations_queue_copy => 0}],
            333),
        { 1 =>  vr_errors('InvalidCampaignType') }
    );
    cmp_validation_result(
        vr([{id => 1, user_id => 123, manager_user_id => undef, agency_user_id=> undef, campaign_type => 'text', status_archived => 'Yes',
                client_id => 333, has_banners => 1, is_in_camp_operations_queue => 0, is_in_camp_operations_queue_copy => 0}],
            333),
        { 1 =>  vr_errors('CampaignArchived') }
    );
    cmp_validation_result(
        vr([{id => 1, user_id => 123, manager_user_id => undef, agency_user_id=> undef, campaign_type => 'text', status_archived => 'No',
                client_id => 333, has_banners => 0, is_in_camp_operations_queue => 0, is_in_camp_operations_queue_copy => 0}],
            333),
        { 1 =>  vr_errors('NoBanners') }
    );
    cmp_validation_result(
        vr([{id => 1, user_id => 123, manager_user_id => undef, agency_user_id=> undef, campaign_type => 'text', status_archived => 'No',
                client_id => 333, has_banners => 1, is_in_camp_operations_queue => 1, is_in_camp_operations_queue_copy => 0}],
            333),
        { 1 =>  vr_errors('AlreadyInCampQueue') }
    );
    cmp_validation_result(
        vr([{id => 1, user_id => 123, manager_user_id => undef, agency_user_id=> undef, campaign_type => 'text', status_archived => 'No',
                client_id => 333, has_banners => 1, is_in_camp_operations_queue => 0, is_in_camp_operations_queue_copy => 1}],
            333),
        { 1 =>  vr_errors('AlreadyInCopyQueue') }
    );
    cmp_validation_result(
        vr([{id => 1, user_id => 123, manager_user_id => undef, agency_user_id=> undef, campaign_type => 'text', status_archived => 'No',
                client_id => 333, has_banners => 0, is_in_camp_operations_queue => 1, is_in_camp_operations_queue_copy => 1},
            {id => 2, user_id => 123, manager_user_id => undef, agency_user_id=> undef, campaign_type => 'text', status_archived => 'No',
                client_id => 333, has_banners => 1, is_in_camp_operations_queue => 1, is_in_camp_operations_queue_copy => 1}],
            333),
        { 1 =>  vr_errors('NoBanners', 'AlreadyInCampQueue', 'AlreadyInCopyQueue'),
            2 =>  vr_errors('AlreadyInCampQueue', 'AlreadyInCopyQueue')}
    );
    cmp_validation_result(
        vr([{id => 1, user_id => 123, manager_user_id => undef, agency_user_id=> undef,  campaign_type => 'text', status_archived => 'No',
               client_id => 333, has_banners => 1, is_in_camp_operations_queue => 0, is_in_camp_operations_queue_copy => 0},
            {id => 2, user_id => 123, manager_user_id => undef, agency_user_id=> undef,  campaign_type => 'text', status_archived => 'No',
               client_id => 333, has_banners => 1, is_in_camp_operations_queue => 0, is_in_camp_operations_queue_copy => 0},
            {id => 3, user_id => 123, manager_user_id => undef, agency_user_id=> undef, campaign_type => 'text', status_archived => 'No',
               client_id => 333, has_banners => 1, is_in_camp_operations_queue => 0, is_in_camp_operations_queue_copy => 0}],
            333),
        vr_errors('LimitExceeded')
    );
};

sub get_campaigns {
    my $campaigns = shift;

    my @campaign_objects;
    for my $campaign (ref $campaigns eq 'ARRAY' ? @$campaigns : $campaigns) {
        push @campaign_objects, Test::Direct::Model::Campaign->new( %$campaign );
    }
    return \@campaign_objects;
}

done_testing;

