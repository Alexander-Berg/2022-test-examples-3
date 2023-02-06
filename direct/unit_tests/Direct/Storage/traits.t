#!/usr/bin/perl

use strict;
use warnings;
use utf8;

use Test::More;
use Test::Exception;
use Test::Yandex::MDS;
use Direct::Storage;

use HashingTools qw/md5_base64ya/;

use Yandex::DBTools;
use Settings;
use Yandex::DBUnitTest qw/:all/;

use Carp;
$SIG{__WARN__} = \&Carp::cluck;

my %db = (
    shard_client_id => {
        original_db => PPCDICT,
        rows => [
            { ClientID => 1, shard => 1 },
        ],
    },
    shard_uid => {
        original_db => PPCDICT,
        rows => [
            { uid => 1, ClientID => 1 },
        ],
    },
    inc_mds_id => {
        original_db => PPCDICT,
        rows => [],
    },
    mds_metadata => {
        original_db => PPC(shard => 'all'),
    },
    mds_custom_names => {
        original_db => PPC(shard => 'all'),
    },
);

my %db2 = (
    # еще одна переменная из-за конфликта имен
    mds_metadata => {
        original_db => PPCDICT,
    },
    mds_custom_names => {
        original_db => PPCDICT,
    },
);

init_test_dataset(\%db);
init_test_dataset(\%db2);


my $server = Test::Yandex::MDS::mock_server(namespace => 'direct-files', authorization => 'hi' );

my $host = $server->uri;
$host =~ s!^.+?://!!;
$host =~ s!/+$!!;
do_sql(PPC(shard => 'all'), "alter table mds_metadata change storage_host storage_host enum(?) not null", $host);
do_sql(PPCDICT, "alter table mds_metadata change storage_host storage_host enum(?) not null", $host);

my $storage = Direct::Storage->new(
    get_host => $host,
    put_host => $host,
    authorization => 'hi',
    namespace => 'direct-files',
);

%Direct::Storage::Types::MDS_FILE_TYPES = (
    name0_id0 => {
        custom_name => 0,     # умираем при непустом filename
        empty_client_id => 0, # умираем при пустом ClientID
    },
    name0_id1 => {
        custom_name => 0, # умираем при непустом filename
        empty_client_id => 1, # Умираем при непустом ClientID
    },
    name1_id0 => {
        custom_name => 1, # умираем при пустом filename
        empty_client_id => 0,
    },
    name1_id1 => {
        custom_name => 1,
        empty_client_id => 1,
    },
);

my $alter = "alter table mds_metadata change type type enum("
    . ( join ",", map { "'$_'" } sort keys %Direct::Storage::Types::MDS_FILE_TYPES )
    . ") not null";

do_sql(PPC(shard => 'all'), $alter);
do_sql(PPCDICT, $alter);

my $content = 'some content';
my $filename = 'my_file';
my $client_id = 1;
my $content_hash = md5_base64ya($content);

dies_ok  { $storage->save('name0_id0', $content) } 'empty ClientID, empty filename';
dies_ok  { $storage->save('name0_id0', $content, filename => $filename) } 'empty ClientID, non-empty filename';
lives_ok { $storage->save('name0_id0', $content, ClientID => 1) } 'non-empty ClientID, empty filename';
dies_ok  { $storage->save('name0_id0', $content, filename => $filename, ClientID => 1) } 'non-empty filename and ClientID';

lives_and { is $storage->get_file('name0_id0', filename => $content_hash, ClientID => 1)->content, $content} 'get by hash, non-empty ClientID';
is $storage->get_file('name0_id0', filename => $filename, ClientID => 1), undef, 'get by filename. non-empty ClientID';
dies_ok { $storage->get_file('name0_id0', filename => $content_hash) } 'get: empty ClientID';

lives_ok { $storage->save('name0_id1', $content) } 'empty ClientID, empty filename';
dies_ok  { $storage->save('name0_id1', $content, filename => $filename) } 'empty ClientID, non-empty filename';
dies_ok  { $storage->save('name0_id1', $content, ClientID => 1) } 'non-empty ClientID, empty filename';
dies_ok  { $storage->save('name0_id1', $content, filename => $filename, ClientID => 1) } 'non-empty filename and ClientID';

lives_and { is $storage->get_file('name0_id1', filename => $content_hash)->content, $content} 'get by hash, empty ClientID';
dies_ok { $storage->get_file('name0_id1', filename => $filename, ClientID => 1) } 'get by filename. non-empty ClientID';
is $storage->get_file('name0_id1', filename => $filename), undef, 'get: empty ClientID';

dies_ok  { $storage->save('name1_id0', $content) } 'empty ClientID, empty filename';
dies_ok  { $storage->save('name1_id0', $content, filename => $filename) } 'empty ClientID, non-empty filename';
dies_ok  { $storage->save('name1_id0', $content, ClientID => 1) } 'non-empty ClientID, empty filename';
lives_ok { $storage->save('name1_id0', $content, filename => $filename, ClientID => 1) } 'non-empty filename and ClientID';

lives_and { is $storage->get_file('name1_id0', filename => $filename, ClientID => 1)->content, $content} 'get';
dies_ok { $storage->get_file('name1_id0', filename => $filename) } 'get by filename. non-empty ClientID';
is $storage->get_file('name1_id0', filename => $content_hash, ClientID => 1), undef, 'get: empty ClientID';

dies_ok  { $storage->save('name1_id1', $content) } 'empty ClientID, empty filename';
lives_ok { $storage->save('name1_id1', $content, filename => $filename) } 'empty ClientID, non-empty filename';
dies_ok  { $storage->save('name1_id1', $content, ClientID => 1) } 'non-empty ClientID, empty filename';
dies_ok  { $storage->save('name1_id1', $content, filename => $filename, ClientID => 1) } 'non-empty filename and ClientID';

lives_and { is $storage->get_file('name1_id1', filename => $filename)->content, $content} 'get';
dies_ok { $storage->get_file('name1_id1', filename => $filename, ClientID => 1) } 'get by filename. non-empty ClientID';
is $storage->get_file('name1_id1', filename => $content_hash), undef, 'get: empty ClientID';

done_testing();

