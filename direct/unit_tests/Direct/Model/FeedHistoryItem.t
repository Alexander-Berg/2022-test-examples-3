#!/usr/bin/perl

use Direct::Modern;

use Test::More;

use Tools qw/encode_json_and_compress/;

BEGIN {
    use_ok('Direct::Model::FeedHistoryItem');
}

subtest "from_db_hash()" => sub {
    subtest "should unpack parse_results compressed json" => sub {
        my $item = Direct::Model::FeedHistoryItem->from_db_hash({
            parse_results_json_compressed => encode_json_and_compress({a => 'b'})
        }, \{});
        is_deeply $item->parse_results, { a => 'b' };
    };

    subtest "should ignore 'NULL' parse_results" => sub {
        my $item = Direct::Model::FeedHistoryItem->from_db_hash({
            parse_results_json_compressed => undef
        }, \{});
        is $item->parse_results, undef;
    };
};

subtest "should serialize parse_results" => sub {
    my $item = Direct::Model::FeedHistoryItem->new(
        parse_results => { c => 'd' },
    );
    is $item->_parse_results_json_compressed, encode_json_and_compress({c => 'd'});
};

done_testing;
