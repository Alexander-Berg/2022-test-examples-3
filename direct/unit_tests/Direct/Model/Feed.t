#!/usr/bin/env perl

use Direct::Modern;

use Test::More;
use Test::Exception;

use Direct::Encrypt qw/decrypt_text/;
use JSON;

BEGIN {
    use_ok('Direct::Model::Feed');
    use_ok('Direct::Model::FeedHistoryItem');
}

sub mk_feed { Direct::Model::Feed->new(@_) }

subtest "Model" => sub {
    lives_ok { mk_feed() };
    lives_ok { mk_feed(history => [Direct::Model::FeedHistoryItem->new()]) };
    dies_ok { mk_feed(history => [mk_feed()]) };
};

subtest "set_password" => sub {
    my $feed = mk_feed();
    $feed->set_password('aaaa');
    is decrypt_text($feed->encrypted_password), 'aaaa';
};

subtest "add_history" => sub {
    my $feed = mk_feed();
    $feed->add_history(Direct::Model::FeedHistoryItem->new());
    is @{$feed->history}, 1;
};

subtest "to_template_hash" => sub {
    is mk_feed(id => 1000)->to_template_hash->{feed_id}, 1000;

    my $feed = mk_feed(
        id => "1000", client_id => "10", refresh_interval => "10",
        fetch_errors_count => "10",  offers_count => "10",
    );
    ok to_json($feed->to_template_hash) !~ /(?:"\d+"|null)/;

    is mk_feed(update_status => 'Done')->to_template_hash->{display_status}, 'Done';
};

done_testing;
