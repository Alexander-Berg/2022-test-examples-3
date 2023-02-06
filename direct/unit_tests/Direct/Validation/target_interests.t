#!/usr/bin/env perl

use Direct::Modern;
use open ':std' => 'utf8';

use Test::More;
use Test::MockModule;

use Settings;

use Direct::Test::DBObjects;
use Direct::TargetingCategories;

BEGIN {
    use_ok 'Direct::Validation::TargetInterests';
}


sub mk_target_interest { shift->new_target_interest_model( @_ ) }
sub vld_target_interest {
    my $x = shift;
    Direct::Validation::TargetInterests::validate_target_interests_for_adgroup(ref($x) eq 'ARRAY' ? $x : [ $x ], @_, { strategy => { is_autobudget => 1, name => 'different_places' } })
}

subtest 'target_interests' => sub {
        Direct::Test::DBObjects->create_tables;
        my $db_objects = Direct::Test::DBObjects->new;
        $db_objects->with_campaign( 'mobile_content' );
        $db_objects->with_adgroup( 'mobile_content' );
        $db_objects->adgroup->active_keywords_count( 1 );
        $db_objects->adgroup->active_retargetings_count( 1 );

        my $module = new Test::MockModule('Direct::TargetingCategories');
        $module->mock( '_get_targeting_categories_from_db', sub {
                [
                    {
                        category_id        => 1,
                        parent_category_id => undef,
                        import_id          => 1,
                        targeting_type     => 'rmp_interest',
                        original_name      => undef,
                        name               => 'some_interest',
                        state              => 'Submitted',
                        order_num          => 0,
                        available          => 1,
                    }, {
                        category_id        => 2,
                        parent_category_id => undef,
                        import_id          => 2,
                        targeting_type     => 'rmp_interest',
                        original_name      => undef,
                        name               => 'other_some_interest',
                        state              => 'Submitted',
                        order_num          => 0,
                        available          => 1,
                    }
                ]
            }
        );

        ok_vr vld_target_interest(mk_target_interest($db_objects, target_category_id => 1), [ ], $db_objects->adgroup);

        subtest 'target_interests_limit_exceeded' => sub {
                local $Direct::Validation::TargetInterests::MAX_TARGET_INTERESTS_IN_ADGROUP = 1;
                err_vr vld_target_interest(
                        [ mk_target_interest($db_objects, target_category_id => 1), mk_target_interest($db_objects, target_category_id => 2) ],
                        [ ],
                        $db_objects->adgroup
                    ), 'LimitExceeded';
            };

        subtest 'target_interests_incorrect_category' => sub {
                err_vr vld_target_interest(
                        mk_target_interest($db_objects, target_category_id => 999),
                        [ ],
                        $db_objects->adgroup
                    ), 'InvalidField';
            };

        subtest 'target_interests_not_uniq_category' => sub {
                err_vr vld_target_interest(
                        [ mk_target_interest($db_objects, target_category_id => 1), mk_target_interest($db_objects, target_category_id => 1) ],
                        [ ],
                        $db_objects->adgroup
                    ), 'InvalidField';
            };

        subtest 'target_interests_already_exists_interest' => sub {
                err_vr vld_target_interest(
                        [ mk_target_interest($db_objects, target_category_id => 1), ],
                        [ mk_target_interest($db_objects, target_category_id => 1), ],
                        $db_objects->adgroup
                    ), 'AlreadyExists';
            };

        subtest 'target_interests_no_active_conditions' => sub {
                $db_objects->adgroup->active_keywords_count( 0 );
                $db_objects->adgroup->active_retargetings_count( 0 );
                ok_vr vld_target_interest(
                        mk_target_interest($db_objects, target_category_id => 1),
                        [ ],
                        $db_objects->adgroup
                    );
            };

    };

done_testing;
