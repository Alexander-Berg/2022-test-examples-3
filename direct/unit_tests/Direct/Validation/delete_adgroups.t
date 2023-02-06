use Direct::Modern;

use Test::More;
use Yandex::Test::ValidationResult;
use Yandex::Test::UTF8Builder;

BEGIN {
    use_ok('Direct::Model::Banner');
    use_ok('Direct::Model::AdGroup');
    use_ok('Direct::Validation::AdGroups', qw/validate_delete_adgroups/);
}

package Test::Direct::Model::Campaign {
    use Mouse;
    extends 'Direct::Model::Campaign';
    with 'Direct::Model::Campaign::Role::BsQueue';
    1;
};

package Test::Direct::Model::AdGroup {
    use Mouse;
    extends 'Direct::Model::AdGroup';
    with 'Direct::Model::Role::Update';
    1;
}

my $vr_1 = validate_delete_adgroups(
    get_adgroups([
        {
            status_moderate => 'Yes',
            status_post_moderate => 'Yes',
            campaign => {
                is_in_bs_queue => 0,
                sum => 874.12,
                status_archived => 'No',
                status_empty => 'No',
            },
            banners => [
                {bs_banner_id => 0, status_moderate => 'New', status_post_moderate => 'No'},
                {bs_banner_id => 0, status_moderate => 'Ready', status_post_moderate => 'No'},
                {bs_banner_id => 0, status_moderate => 'No', status_post_moderate => 'No'}
            ]
        },
        {
            status_moderate => 'New',
            status_post_moderate => 'No',
            campaign => {
                is_in_bs_queue => 1,
                sum => 874.12,
                status_archived => 'No',
                status_empty => 'No',
            },
            banners => [
                {bs_banner_id => 0, status_moderate => 'New', status_post_moderate => 'No'},
                {bs_banner_id => 0, status_moderate => 'New', status_post_moderate => 'No'},
                {bs_banner_id => 0, status_moderate => 'New', status_post_moderate => 'No'}
            ]
        },
        {
            status_moderate => 'New',
            status_post_moderate => 'No',
            campaign => {
                is_in_bs_queue => 1,
                sum => 0,
                status_archived => 'No',
                status_empty => 'No',
            },
            banners => [
                {bs_banner_id => 0, status_moderate => 'New', status_post_moderate => 'No'},
            ]
        }
    ])
);
ok_validation_result($vr_1);

my $vr_2 = validate_delete_adgroups(
    get_adgroups([
        # кампания с деньгами и есть промодерированые баннеры
        {
            status_moderate => 'Yes',
            status_post_moderate => 'Yes',
            campaign => {
                is_in_bs_queue => 0,
                sum => 874.12,
                status_archived => 'No',
                status_empty => 'No',
            },
            banners => [
                {bs_banner_id => 0, status_moderate => 'Yes', status_post_moderate => 'Yes'},
                {bs_banner_id => 0, status_moderate => 'New', status_post_moderate => 'No'},
                {bs_banner_id => 0, status_moderate => 'No', status_post_moderate => 'Rejected'}
            ]
        },
        # кампания с деньгами, все баннеры черновики - удалить можно
        {
            status_moderate => 'New',
            status_post_moderate => 'No',
            campaign => {
                is_in_bs_queue => 0,
                sum => 874.12,
                status_archived => 'No',
                status_empty => 'No',
            },
            banners => [
                {bs_banner_id => 0, status_moderate => 'New', status_post_moderate => 'No'},
                {bs_banner_id => 0, status_moderate => 'New', status_post_moderate => 'No'},
            ]
        },
        # кампания в очереди на экспорт БК
        {
            status_moderate => 'Yes',
            status_post_moderate => 'Yes',
            campaign => {
                is_in_bs_queue => 1,
                sum => 874.12,
                status_archived => 'No',
                status_empty => 'No',
            },
            banners => [
                {bs_banner_id => 0, status_moderate => 'Yes', status_post_moderate => 'Yes'},
                {bs_banner_id => 0, status_moderate => 'No', status_post_moderate => 'Rejected'},
            ]
        },
        # баннеры отправлены в БК
        {
            status_moderate => 'Yes',
            status_post_moderate => 'Yes',
            campaign => {
                is_in_bs_queue => 0,
                sum => 874.12,
                status_archived => 'No',
                status_empty => 'No',
            },
            banners => [
                {bs_banner_id => 0, status_moderate => 'Yes', status_post_moderate => 'Yes'},
                {bs_banner_id => 88921, status_moderate => 'No', status_post_moderate => 'No'},
            ]
        },
    ])
);

cmp_validation_result($vr_2, [
    vr_errors('BadStatus'),
    {},
    vr_errors('BadStatus'),
    vr_errors('BadStatus'),
]);

my $vr_3 = validate_delete_adgroups(
    # кампания в очереди в БК, есть принятые баннеры
    get_adgroups([
        {
            status_moderate => 'Yes',
            status_post_moderate => 'Yes',
            campaign => {
                is_in_bs_queue => 1,
                sum => 0,
                status_archived => 'No',
                status_empty => 'No',
            },
            banners => [
                {bs_banner_id => 0, status_moderate => 'Yes', status_post_moderate => 'Yes'},
                {bs_banner_id => 0, status_moderate => 'Yes', status_post_moderate => 'Yes'},
                {bs_banner_id => 0, status_moderate => 'Ready', status_post_moderate => 'Rejected'}
            ]
        }
    ])
);
cmp_validation_result($vr_3, [
    vr_errors('BadStatus'),
]);

my $vr_4 = validate_delete_adgroups(
    # кампания в очереди в БК, есть принятые баннеры
    get_adgroups([
        {
            status_moderate => 'Yes',
            status_post_moderate => 'Yes',
            campaign => {
                is_in_bs_queue => 1,
                sum => 673.02,
                status_archived => 'No',
                status_empty => 'No',
            },
            banners => [
                {bs_banner_id => 991, status_moderate => 'Yes', status_post_moderate => 'Yes'},
            ]
        },
        {
            status_moderate => 'Yes',
            status_post_moderate => 'Yes',
            campaign => {
                is_in_bs_queue => 0,
                sum => 673.02,
                status_archived => 'No',
                status_empty => 'No',
            },
            banners => [
                {bs_banner_id => 991, status_moderate => 'Yes', status_post_moderate => 'Yes'},
            ]
        }
    ])
);
cmp_validation_result($vr_4, [
    vr_errors('BadStatus'),
    vr_errors('BadStatus'),
]);

my $vr_5 = validate_delete_adgroups(
    # проверяем удаление пустых групп
    get_adgroups([
        {
            id => 1,
            banners_count => 0,
            has_show_conditions => 0,
        },
        {
            id => 2,
            banners_count => 3,
            has_show_conditions => 0
        },
        {
            id => 3,
            banners_count => 0,
            has_show_conditions => 1,
        },
        {
            id => 4,
            banners_count => 5,
            has_show_conditions => 1,
        },
    ])
);

cmp_validation_result(
    $vr_5,
    [
            {},
            vr_errors('CantDelete'),
            vr_errors('CantDelete'),
            vr_errors('CantDelete'),
    ],
    'validate delete empty adgroups'
);

done_testing;

sub get_adgroups {
    my $groups = shift;

    my @group_objects;
    for my $group (ref $groups eq 'ARRAY' ? @$groups : $groups) {
        my @banners = map { Direct::Model::Banner->new(%$_) } @{$group->{banners} || []};
        delete $group->{banners};
        push @group_objects, Test::Direct::Model::AdGroup->new(
            exists $group->{campaign} ? (campaign => Test::Direct::Model::Campaign->new(%{delete $group->{campaign}})) : (),
            %$group,
            @banners ? (banners => \@banners) : (),
        );
    }
    return \@group_objects;
}
