#!/usr/bin/env perl

use Direct::Modern;
use open ':std' => 'utf8';

use Carp;
use JSON;
use Test::More;
use Test::MockModule;

use Settings;

use Yandex::DBTools;

use Direct::Test::DBObjects;
use Direct::TargetingCategories;
use Direct::TargetInterests;

use Test::JavaIntapiMocks::GenerateObjectIds;

subtest 'create_target_interest' => sub {
        Direct::Test::DBObjects->create_tables;
        my $db_objects = Direct::Test::DBObjects->new;
        $db_objects->with_campaign( 'mobile_content' );
        $db_objects->with_adgroup( 'mobile_content' );

        my $module = new Test::MockModule('Direct::TargetingCategories');
        $module->mock( '_get_targeting_categories_from_db', sub {
                [
                    {
                        category_id        => 1,
                        parent_category_id => undef,
                        import_id          => 777,
                        targeting_type     => 'rmp_interest',
                        original_name      => undef,
                        name               => 'some_interest',
                        state              => 'Submitted',
                        order_num          => 0,
                    },
                ]
            }
        );

        my $target_interest = $db_objects->new_target_interest_model( target_category_id => 1 );
        $target_interest->last_change( 'now' );
        local $LogTools::context{uid} = $db_objects->user->id;
        Direct::TargetInterests->new( items => [ $target_interest ] )->create();

        my $result = get_one_line_sql(
            PPC(shard => $db_objects->shard), [
                'SELECT rc.condition_json, rg.goal_id FROM bids_retargeting br JOIN retargeting_conditions rc USING(ret_cond_id) JOIN retargeting_goals rg USING(ret_cond_id)',
                where => [ 'br.pid' => $db_objects->adgroup->id ],
                'LIMIT 1'
            ]
        );
        ok $result->{goal_id} == 777;
        my $condition_json;
        eval {
            $condition_json = JSON->new->utf8( 0 )->decode( $result->{condition_json} );
            1;
        } or do { croak "Cannot apply `condition_json`: $@"; };

        is_deeply($condition_json, [ { type => 'all', goals => [ { goal_id => 777, time => 90 } ] } ])
    };

done_testing;
