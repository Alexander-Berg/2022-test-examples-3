#!/usr/bin/perl

use strict;
use warnings;
use utf8;

use Test::More;
use Test::Exception;
use Test::Yandex::MDS;
use Direct::Storage;

use HashingTools qw/md5_base64ya/;

use Settings;
use Yandex::DBUnitTest qw/:all/;
use Yandex::DBTools;
use PSGIApp::Storage;

# use Carp;
# $SIG{__WARN__} = \&Carp::cluck;
# $SIG{__DIE__} = \&Carp::confess;

my %db = (
    shard_client_id => {
        original_db => PPCDICT,
        rows => [
            { ClientID => 1, shard => 1 },
        ],
    },
    inc_mds_id => {
        original_db => PPCDICT,
        rows => [],
    },
    mds_metadata => {
        original_db => PPC(shard => 'all'),
    },
);

init_test_dataset(\%db);

my $server = Test::Yandex::MDS::mock_server(namespace => 'direct-files', authorization => 'hi' );

my $host = $server->uri;
$host =~ s!^.+?://!!;
$host =~ s!/+$!!;

do_sql(PPC(shard => 'all'), "alter table mds_metadata change storage_host storage_host enum(?) not null", $host);

my $storage = Direct::Storage->new(
    get_host => $host,
    put_host => $host,
    authorization => 'hi',
    namespace => 'direct-files',
);

my $clid = 1;
my $type = 'perf_feeds';

my $feed = 'some feed data here: '.rand();

my $hash = md5_base64ya($feed);

dies_ok { $storage->save('xxx-invalid-type-xxx', $feed, ClientID => $clid) } 'dies on invalid type';
dies_ok { $storage->save($type, $feed, ClientID => undef) } 'dies on invalid client_id';

{
    my $storage = Direct::Storage->new(
        get_host => $host,
        put_host => $host,
        authorization => 'bye',
        namespace => 'direct-files',
    );
    dies_ok { $storage->save($type, $feed, ClientID => $clid) } 'dies on invalid auth';
}

my $feed_file = $storage->get_file($type, ClientID => $clid, filename => $hash);
is($feed_file, undef, 'file for non-existing file is undef');

$feed_file = $storage->save($type, $feed, ClientID => $clid);
my $key = $feed_file->_mds_key;
ok($key, "save returned non-empty key");

ok($feed eq $feed_file->content, "file->content");

$storage->delete_file($type, ClientID => $clid, filename => $hash);

is($storage->get_file($type, ClientID => $clid, filename => $hash), undef, "get_file returns undef after delete_file");

# dies_ok { $storage->get_or_save($type, $feed, ClientID => 'invalid clid') } 'get_or_save dies on invalid input';

my $public_type = 'banner_images_uploads';

{
my $file = $storage->save($public_type, 'some image data here', ClientID => $clid);

my $file_for_web = PSGIApp::Storage::respond_storage_file($clid, $public_type, $file->filename);
is($file_for_web->[0], 200, "respond_storage_file returns 200 OK for public type");
}


{
local $SIG{__WARN__} = sub {};
my $file_for_web = PSGIApp::Storage::respond_storage_file($clid, $type, 'some feed');
is($file_for_web->[0], 403, "respond_storage_file returns 403 for private type");
}

done_testing();

