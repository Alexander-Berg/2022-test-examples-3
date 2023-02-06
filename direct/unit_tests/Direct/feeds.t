#!/usr/bin/env perl

use Direct::Modern;

use Test::More;
use Test::Exception;
use Test::MockObject;
use Test::CreateDBObjects;

use Settings;

use Yandex::DBShards;
use Yandex::DBTools;
use List::MoreUtils qw/uniq/;

use Direct::Test::DBObjects;
use Direct::Storage;

BEGIN {
    use_ok 'Direct::Feeds';

    no warnings 'redefine';
    *Direct::Storage::new = sub { bless {}, 'Direct::Storage::Mock' }
}

package Direct::Storage::Mock {
    use base qw/Direct::Storage/;
    sub save {
        my ($self) = @_;
        return $self->{_save} if defined $self->{_save};
        return Test::MockObject->new()
            -> set_always(url => 'http://somewhere.in.mds')
            -> set_always(filename => 'the_filename');
    }
};

sub mk_feed {
    Direct::Model::Feed->new(
        client_id => create('user')->{ClientID},
        source => 'url',
        name => 'Test Feed',
        refresh_interval => 86400,
        update_status => 'New',
        @_
    );
}

create_tables();

subtest "save() for a new feed" => sub {
    subtest "should insert new feed to database" => sub {
        my $feed = mk_feed();
        Direct::Feeds->new(items => [$feed])->save();
        is_one_field PPC(ClientID => $feed->client_id), ["select count(*) from feeds", where => {ClientID => SHARD_IDS}], 1;
    };

    subtest "should update 'id' field in model" => sub {
        my $feed = mk_feed();
        Direct::Feeds->new(items => [$feed])->save;
        ok $feed->id > 0;
    };

    subtest "should set update_status to 'New'" => sub {
        my $feed = mk_feed();
        Direct::Feeds->new(items => [$feed])->save;
        is_one_field PPC(ClientID => $feed->client_id), ["select update_status from feeds", where => {ClientID => SHARD_IDS}], "New";
    };

    subtest "should send 'content' to MDS when it is set" => sub {
        my $storage = Direct::Storage::Mock->new();
        $storage->{_save} = Test::MockObject->new()
            -> set_always(url => 'http://somewhere.in.mds/x.file')
            -> set_always(filename => 'the_filename');

        my $feed = mk_feed(source => 'file', content => 'test123');

        Direct::Feeds->new(items => [$feed], storage => $storage)->save;

        is $feed->url, 'http://somewhere.in.mds/x.file';
        ok $feed->cached_file_hash;
    };

    subtest "should set LastChange" => sub {
        my $feed = mk_feed();
        Direct::Feeds->new(items => [$feed])->save;
        my $last_change_delta = get_one_field_sql(PPC(ClientID => $feed->client_id), [
            "select unix_timestamp(now()) - unix_timestamp(LastChange) from feeds", where => {feed_id => $feed->id},
        ]);
        ok $last_change_delta <= 1;
    };

    subtest "should insert all known history items into DB" => sub {
        my $feed = mk_feed();
        $feed->add_history(Direct::Model::FeedHistoryItem->new());
        Direct::Feeds->new(items => [$feed])->save;
        is_one_field PPC(ClientID => $feed->client_id), ["select count(*) from perf_feed_history", where => {feed_id => $feed->id}], 1;
    };

    subtest "should set default refresh interval for 'url' feed" => sub {
        my $feed = mk_feed(source => 'url');
        Direct::Feeds->new(items => [$feed])->save;
        is_one_field PPC(ClientID => $feed->client_id), ["select refresh_interval from feeds", where => {feed_id => $feed->id}], $Settings::DEFAULT_FEED_REFRESH_INTERVAL;
    };

    subtest "should set zero refresh interval for 'file' feed" => sub {
        my $feed = mk_feed(source => 'file', content => 'test123');
        Direct::Feeds->new(items => [$feed])->save;
        is_one_field PPC(ClientID => $feed->client_id), ["select refresh_interval from feeds", where => {feed_id => $feed->id}], 0;
    };
};

subtest "save() for an existing feed" => sub {
    subtest "should update feed in the database" => sub {
        my $feed = create('perf_feed');
        $feed->name('RajMonVef4');

        Direct::Feeds->new(items => [$feed])->save;

        is_one_field PPC(ClientID => $feed->client_id), ["select name from feeds", where => {ClientID => SHARD_IDS}], 'RajMonVef4';
    };

    subtest "should ignore 'content' when its hash matches that in cached_file_hash" => sub {
        my $feed = create('perf_feed', source => 'file', content => 'abc', update_status => 'Done');

        $feed = Direct::Feeds->get_by($feed->client_id, id => $feed->id)->items->[0]; # Reload from database with a fresh state

        $feed->content('abc');
        Direct::Feeds->new(items => [$feed])->save;

        # update_status was not touched
        is_one_field PPC(ClientID => $feed->client_id), ["select update_status from feeds", where => {ClientID => SHARD_IDS}], 'Done';
    };

    subtest "should set update_status to 'New' and send 'content' to MDS when new file is uploaded" => sub {
        my $feed = create('perf_feed', source => 'file', content => 'abc', update_status => 'Done');
        $feed = Direct::Feeds->get_by($feed->client_id, id => $feed->id)->items->[0]; # Reload from database with a fresh state

        my $storage = Direct::Storage::Mock->new();
        $storage->{_save} = Test::MockObject->new()
            -> set_always(url => 'http://somewhere.in.mds')
            -> set_always(filename => 'new file hash');

        my $feed_logic = Direct::Feeds->new(items => [$feed], storage => $storage);

        $feed->content('def');
        $feed_logic->save;

        is_one_field PPC(ClientID => $feed->client_id), ["select update_status from feeds", where => {ClientID => SHARD_IDS}], 'New';
        ok $storage->{_save}->called('filename');
    };

    subtest "setting name should update LastChange" => sub {
        my $feed = create('perf_feed', source => 'file', content => 'abc', update_status => 'Done', last_change_long_ago => 1);

        $feed->name('RajMonVef4');
        Direct::Feeds->new(items => [$feed])->save;

        my $last_change_delta = get_one_field_sql(PPC(ClientID => $feed->client_id), [
            "select unix_timestamp(now()) - unix_timestamp(LastChange) from feeds", where => {feed_id => $feed->id},
        ]);

        ok $last_change_delta <= 1;
    };

    subtest "cannot change source" => sub {
        my $feed = create('perf_feed', refresh_interval => 300, source => 'url');
        $feed->source('file');
        dies_ok { Direct::Feeds->new(items => [$feed])->save };
    };
};

subtest "get_history()" => sub {
    subtest "should return array of Direct::Model::FeedHistoryItem" => sub {
        my $feed = create('perf_feed', with_history => 10);
        my $history = Direct::Feeds->get_history($feed->client_id, $feed->id);
        is @$history, 10;
    };
    subtest "should return total count in list context" => sub {
        my $feed = create('perf_feed', with_history => 10);
        my ($history, $total_count) = Direct::Feeds->get_history($feed->client_id, $feed->id);
        is $total_count, 10;
    };

    subtest "should handle 'limit' and 'offset' parameters" => sub {
        my $feed = create('perf_feed', with_history => 10);
        my $part_1 = Direct::Feeds->get_history($feed->client_id, $feed->id, limit => 5);
        my $part_2 = Direct::Feeds->get_history($feed->client_id, $feed->id, offset => 5, limit => 2);
        my $part_3 = Direct::Feeds->get_history($feed->client_id, $feed->id, offset => 7, limit => 100);

        # Parts has proper count of items
        is @$part_1, 5;
        is @$part_2, 2;
        is @$part_3, 3;

        # And don't overlap
        is((uniq map { $_->id } @$part_1, @$part_2, @$part_3), 10);
    };

    subtest "totals should be calculated even with limit" => sub {
        my $feed = create('perf_feed', with_history => 10);
        my ($items, $totals) = Direct::Feeds->get_history($feed->client_id, $feed->id, limit => 5);
        is @$items, 5;
        is $totals, 10;
    };
};

subtest "check_add_limit()" => sub {
    subtest "allows to add first feed" => sub {
        my $user = create('user');
        ok !Direct::Feeds->check_add_limit(client_id => $user->{ClientID});
    };
    subtest "properly handles default limit" => sub {
        my $user = create('user');
        my @feeds = map { create('perf_feed', uid =>  $user->{uid}) } 1..50;
        ok(Direct::Feeds->check_add_limit(client_id => $user->{ClientID}));
    };
    subtest "properly handles custom limit" => sub {
        my $user = create('user');
        exec_sql(PPC(ClientID => $user->{ClientID}), [
            "insert into client_limits(ClientID, feed_count_limit) values(?, 30)"
        ], $user->{ClientID});
        my @feeds = map { create('perf_feed', uid =>  $user->{uid}) } 1..25;
        ok !Direct::Feeds->check_add_limit(client_id => $user->{ClientID});

        my @morefeeds = map { create('perf_feed', uid =>  $user->{uid}) } 1..5;
        ok(Direct::Feeds->check_add_limit(client_id => $user->{ClientID}));
    };
};

subtest "delete_unused()" => sub {
    subtest "could remove single unused feed" => sub {
        for my $status (qw/Done Error/) {
            subtest "in $status status" => sub {
                my $feed = create('perf_feed', update_status => $status);
                is_deeply(Direct::Feeds->delete_unused($feed->client_id, [$feed->id]), {$feed->id => "deleted"});
                is_one_field PPC(ClientID => $feed->client_id), ["select count(*) from feeds", where => {ClientID => $feed->client_id}], 0;
            }
        }
    };
    subtest "returns 'used' for a feed in state other than 'Done' or 'Error'" => sub {
        for my $status (qw/New Outdated Updating/) {
            subtest "in $status status" => sub {
                my $feed = create('perf_feed', update_status => $status);
                is_deeply(Direct::Feeds->delete_unused($feed->client_id, [$feed->id]), {$feed->id => "used"});
                is_one_field PPC(ClientID => $feed->client_id), ["select count(*) from feeds", where => {ClientID => $feed->client_id}], 1;
            }
        }
    };
    subtest "returns 'not_found' for non-existent feed" => sub {
        my $user = create('user');
        is_deeply(Direct::Feeds->delete_unused($user->{ClientID}, [1]), {1 => "not_found"});
    };
    subtest "returns 'not_found' for a feed of wrong client" => sub {
        my $user = create('user', shard => 1);
        my $other_user = create('user', shard => 1);
        my $feed = create('perf_feed', uid => $other_user->{uid});
        is_deeply(Direct::Feeds->delete_unused($user->{ClientID}, [$feed->id]), {$feed->id => "not_found"});
    };
    subtest "returns 'used' for a feed that is mentioned in some adgroups_performance" => sub {
        my $user = create('user');
        my $feed = create('perf_feed', uid =>  $user->{uid});
        my $group = create('group', uid => $user->{uid});
        exec_sql(PPC(ClientID => $user->{ClientID}), "insert into adgroups_performance (pid, feed_id) values (?, ?)", $group->{pid}, $feed->id);

        is_deeply(Direct::Feeds->delete_unused($feed->client_id, [$feed->id]), {$feed->id => "used"});
        is_one_field PPC(ClientID => $feed->client_id), ["select count(*) from feeds", where => {ClientID => $feed->client_id}], 1;
    };
    subtest "deletes feed history along with feed itself" => sub {
        my $feed = mk_feed(update_status => 'Done');
        $feed->add_history(Direct::Model::FeedHistoryItem->new());
        Direct::Feeds->new(items => [$feed])->save;
        my $result = Direct::Feeds->delete_unused($feed->client_id, [$feed->id]);
        is_one_field PPC(ClientID => $feed->client_id), ["select count(*) from perf_feed_history", where => {feed_id => $feed->id}], 0;
    };
    subtest "deletes feed categories/vendors along with feed itself" => sub {
        my $feed = mk_feed(update_status => 'Done');
        Direct::Feeds->new(items => [$feed])->save;
        do_insert_into_table(PPC(ClientID => $feed->client_id), 'perf_feed_categories', {
            feed_id => $feed->id,
            category_id => 10,
            parent_category_id => 11,
            name => 'ho',
        });
        do_insert_into_table(PPC(ClientID => $feed->client_id), 'perf_feed_vendors', {
            feed_id => $feed->id,
            name => 'ha',
        });
        my $result = Direct::Feeds->delete_unused($feed->client_id, [$feed->id]);
        is_one_field PPC(ClientID => $feed->client_id), ["select count(*) from perf_feed_categories", where => {feed_id => $feed->id}], 0;
        is_one_field PPC(ClientID => $feed->client_id), ["select count(*) from perf_feed_vendors", where => {feed_id => $feed->id}], 0;
    }
};

subtest "get_by()" => sub {
    subtest "should support limit/offset" => sub {
        my $user = create('user');
        my @created_feeds = map { create('perf_feed', uid => $user->{uid}) } 1..10;
        my $part_1 = Direct::Feeds->get_by($user->{ClientID}, limit => 5)->items;
        my $part_2 = Direct::Feeds->get_by($user->{ClientID}, limit => 3, offset => 5)->items;
        my $part_3 = Direct::Feeds->get_by($user->{ClientID}, limit => 100, offset => 8)->items;
        is @$part_1, 5;
        is @$part_2, 3;
        is @$part_3, 2;
        is((uniq map { $_->id } @$part_1, @$part_2, @$part_3), 10);
    };
    subtest "'filter' should filter by feed_id" => sub {
        plan skip_all => "Test is unstable due to infix matching on feed_id";
        my $user = create('user');
        my @created_feeds = map { create('perf_feed', uid => $user->{uid}) } 1..50;
        my $feeds = Direct::Feeds->get_by($user->{ClientID}, filter => $created_feeds[-1]{id})->items;
        is @$feeds, 1;
    };
    subtest "'filter' should filter by feed name" => sub {
        my $user = create('user');
        my @created_feeds = map { create('perf_feed', uid => $user->{uid}, name => "Test FEED$_") } 1..50;
        my $feeds = Direct::Feeds->get_by($user->{ClientID}, filter => {name__contains => "t FEED5"})->items;
        is_deeply [sort map { $_->name } @$feeds], ["Test FEED5", "Test FEED50"];
    };
    subtest "'id' should return feeds with given identifiers" => sub {
        my $user = create('user');
        my @created_feeds = map { create('perf_feed', uid => $user->{uid}, name => "Test FEED$_") } 1..50;
        my $feeds = Direct::Feeds->get_by($user->{ClientID}, id => [$created_feeds[10]->id, $created_feeds[20]->id])->items;
        is_deeply [sort map { $_->name } @$feeds], ["Test FEED11", "Test FEED21"];
    };
    subtest "should sort by field in ascending order" => sub {
        my $user = create('user');
        my @feeds = map { create('perf_feed', uid => $user->{uid}, name => $_) } qw/hjk abd def/;
        my $feeds = Direct::Feeds->get_by($user->{ClientID}, sort => {name => 'asc'})->items;
        is_deeply [map { $_->name } @$feeds], [qw/abd def hjk/];
    };
    subtest "should sort by field in descending order" => sub {
        my $user = create('user');
        my @feeds = map { create('perf_feed', uid => $user->{uid}, name => $_) } qw/hjk abd def/;
        my $feeds = Direct::Feeds->get_by($user->{ClientID}, sort => {name => 'desc'})->items;
        is_deeply [map { $_->name } @$feeds], [qw/hjk def abd/];
    };
    subtest "should return totals when asked for" => sub {
        my $user = create('user');
        my @created_feeds = map { create('perf_feed', uid => $user->{uid}) } 1..10;
        my $feeds = Direct::Feeds->get_by($user->{ClientID}, limit => 5, total_count => 1);
        is @{$feeds->items}, 5;
        is $feeds->total, 10;
    };
    subtest "should return campaigns where feed is used when 'with_campaigns' option is specified" => sub {
        my $user = create('user');
        my $feed = create('perf_feed', uid => $user->{uid});
        my $group = create('group', uid => $user->{uid});
        exec_sql(PPC(ClientID => $user->{ClientID}), "insert into adgroups_performance (pid, feed_id) values (?, ?)", $group->{pid}, $feed->id);
        my $feeds = Direct::Feeds->get_by($user->{ClientID}, with_campaigns => 1)->items;
        is $feeds->[0]->campaigns->[0]->id, $group->{cid};
    };
    subtest "should return at least empty list of campaigns with 'with_campaigns' option" => sub {
        my $feed = create('perf_feed');
        my $feeds = Direct::Feeds->get_by($feed->client_id, with_campaigns => 1)->items;
        is @{$feeds->[0]->campaigns}, 0;
    };
    subtest "should return total count when 'with_campaigns' option is used" => sub {
        my $user = create('user');
        my @feeds = map { create('perf_feed', uid => $user->{uid}) } 1..2;
        my $feeds = Direct::Feeds->get_by($user->{ClientID}, with_campaigns => 1, total_count => 1);
        is $feeds->total, 2;
    };
    subtest "should handle db/model field name mismatch in sort field" => sub {
        my $feed = create('perf_feed');
        lives_ok {
            Direct::Feeds->get_by($feed->client_id, sort => {'last_change' => 'asc'});
        };
    };
    subtest "should avoid cyclic categories" => sub {
        my $feed = create('perf_feed');

        do_insert_into_table(PPC(ClientID => $feed->client_id), 'perf_feed_categories', {
            feed_id => $feed->id, category_id => 1, parent_category_id => 3, name => 'cat1',
        });
        do_insert_into_table(PPC(ClientID => $feed->client_id), 'perf_feed_categories', {
            feed_id => $feed->id, category_id => 2, parent_category_id => 1, name => 'cat2',
        });
        do_insert_into_table(PPC(ClientID => $feed->client_id), 'perf_feed_categories', {
            feed_id => $feed->id, category_id => 3, parent_category_id => 2, name => 'cat2',
        });

        my $feeds = Direct::Feeds->get_with_categories($feed->client_id)->items;
        is_deeply $feeds->[0]->categories->[0]->{path}, [qw/1 2/];
        is_deeply $feeds->[0]->categories->[1]->{path}, [qw/2 0/];
        is_deeply $feeds->[0]->categories->[2]->{path}, [qw/0 1/];
    };
    subtest "check sub _enrich_with_categories" => sub {

        my $cases = [[
            {category_id => 1, parent_category_id => 2, path => [qw/1/]},
            {category_id => 2, parent_category_id => 0, path => []},
           ],[
            {category_id => 1, parent_category_id => 2, path => [qw/2 1/]},
            {category_id => 2, parent_category_id => 3, path => [qw/2/]},
            {category_id => 3, parent_category_id => 0, path => []},
           ],[
            {category_id => 1, parent_category_id => 3, path => [qw/3 2/]},
            {category_id => 2, parent_category_id => 3, path => [qw/3 2/]},
            {category_id => 3, parent_category_id => 4, path => [qw/3/]},
            {category_id => 4, parent_category_id => 0, path => []},
            {category_id => 5, parent_category_id => 4, path => [qw/3/]},
           ],[
            {category_id => 1, parent_category_id => 2, path => [qw/2 1/]},
            {category_id => 2, parent_category_id => 3, path => [qw/2/]},
            {category_id => 3, parent_category_id => 0, path => []},
            {category_id => 4, parent_category_id => 5, path => [qw/2 1 0 5 4/]},
            {category_id => 5, parent_category_id => 6, path => [qw/2 1 0 5/]},
            {category_id => 6, parent_category_id => 1, path => [qw/2 1 0/]},
            {category_id => 7, parent_category_id => 2, path => [qw/2 1/]},
           ],[
            {category_id => 1, parent_category_id => 5, path => [qw/2 1 3 5 4/]},
            {category_id => 2, parent_category_id => 3, path => [qw/3 2/]},
            {category_id => 3, parent_category_id => 4, path => [qw/1 3/]},
            {category_id => 4, parent_category_id => 2, path => [qw/2 1/]},
            {category_id => 5, parent_category_id => 6, path => [qw/2 1 3 5/]},
            {category_id => 6, parent_category_id => 4, path => [qw/2 1 3/]},
           ],[
            {category_id => 1, parent_category_id => 7, path => [qw/2 1 3 4/]},
            {category_id => 2, parent_category_id => 3, path => [qw/3 2/]},
            {category_id => 3, parent_category_id => 4, path => [qw/1 3/]},
            {category_id => 4, parent_category_id => 2, path => [qw/2 1/]},
            {category_id => 7, parent_category_id => 4, path => [qw/2 1 3/]},
           ],[
            {category_id => 1, parent_category_id => 4, path => [qw/2 1 3/]},
            {category_id => 2, parent_category_id => 3, path => [qw/3 2/]},
            {category_id => 3, parent_category_id => 4, path => [qw/1 3/]},
            {category_id => 4, parent_category_id => 2, path => [qw/2 1/]},
            {category_id => 5, parent_category_id => 2, path => [qw/3 2 1/]},
            {category_id => 6, parent_category_id => 5, path => [qw/3 2 1 4/]},
           ],[
            {category_id => 1, parent_category_id => 1, path => []},
           ],[
            {category_id => 1, parent_category_id => 2, path => [qw/1/]},
            {category_id => 2, parent_category_id => 1, path => [qw/0/]},
           ],[
            {category_id => 1, parent_category_id => 3, path => [qw/2/]},
            {category_id => 2, parent_category_id => 4, path => [qw/3/]},
            {category_id => 3, parent_category_id => 1, path => [qw/0/]},
            {category_id => 4, parent_category_id => 2, path => [qw/1/]},
           ],[
            {category_id => 5, parent_category_id => 10, path => [qw/3 1 4 2/]},
            {category_id => 9, parent_category_id => 15, path => [qw/4 2 0 3/]},
            {category_id => 10, parent_category_id => 17, path => [qw/0 3 1 4/]},
            {category_id => 15, parent_category_id => 5, path => [qw/1 4 2 0/]},
            {category_id => 17, parent_category_id => 9, path => [qw/2 0 3 1/]},
           ],[
            {category_id => 1, parent_category_id => 3, path => [qw/5 6 9 3 2/]},
            {category_id => 2, parent_category_id => 3, path => [qw/5 6 9 3 2/]},
            {category_id => 3, parent_category_id => 4, path => [qw/5 6 9 3/]},
            {category_id => 4, parent_category_id => 10, path => [qw/5 6 9/]},
            {category_id => 5, parent_category_id => 6, path => [qw/6 9 3 5/]},
            {category_id => 6, parent_category_id => 4, path => [qw/6 9 3/]},
            {category_id => 7, parent_category_id => 6, path => [qw/9 3 5/]},
            {category_id => 8, parent_category_id => 7, path => [qw/9 3 5 6/]},
            {category_id => 9, parent_category_id => 8, path => [qw/9 3 5 6 7/]},
            {category_id => 10, parent_category_id => 7, path => [qw/3 5 6/]},
           ],
        ];
        foreach my $data (@$cases) {
            my $feed = create('perf_feed');
            foreach my $d (@$data) {
                do_insert_into_table(PPC(ClientID => $feed->client_id), 'perf_feed_categories', {
                    feed_id => $feed->id, category_id => $d->{category_id}, parent_category_id => $d->{parent_category_id}, name => 'cat',
                });
            }
            my $feeds = Direct::Feeds->get_with_categories($feed->client_id)->items;
            foreach my $i (0..$#$data) {
                is_deeply $feeds->[0]->categories->[$i]->{path}, $data->[$i]->{path};
            }
        }
    };
};

subtest "items_by()" => sub {
    subtest "groups by feed id" => sub {
        my $user = create('user');
        my @created_feeds = map { create('perf_feed', uid => $user->{uid}) } 1..10;
        my $by_id = Direct::Feeds->get_by($user->{ClientID}, limit => 5)->items_by('id');
        ok $by_id->{$created_feeds[0]->id}->id;
    };
};

subtest 'Remote UTM' => sub {
    my $feed = Direct::Model::Feed->new(
        id => 1 , source => 'url', url => 'http://ya.ru', update_status => 'Done',
        login => 'login', encrypted_password => '', cached_file_hash => '', is_remove_utm => 0,
        last_change => '2016-01-01 00:00:00'
    );
    sandbox $feed => sub {
        Direct::Feeds->new( items => [ $_ ] )->prepare_update();
        is_deeply( $_->get_state_hash, {
            changes => {},
            flags   => {},
        } );
        is $_->update_status, 'Done';
    };
    sandbox $feed => sub {
        $_->is_remove_utm(1);
        Direct::Feeds->new( items => [ $_ ] )->prepare_update();
        is_deeply( $_->get_state_hash, {
            changes => { last_change => 1, is_remove_utm => 1 , update_status => 1 },
            flags   => { bs_sync_banners => 1 },
        } );
        is $_->update_status, 'New';
    };
};

done_testing;
