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
$SIG{__DIE__} = \&Carp::confess;

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

my $type = 'common_file_export';
my $content = 'some report data';
my $filename = 'the_cool_name.xml';

{
my $file = $storage->save($type, $content, filename => $filename);
is($file->content, $content, "custom file is saved");
is($file->filename, $filename, "valid custom filename");
}

{
$storage->save($type, $content.'--new1', filename => $filename.'1');
my $file = $storage->get_file($type, filename => $filename.'1');
is($file->content, $content.'--new1', 'save new file with old name');

$storage->save($type, $content.'--new2', filename => $filename.'1');
$file = $storage->get_file($type, filename => $filename.'1');
is($file->content, $content.'--new2', 'overwrite with custom name');

}

{
my $file = $storage->get_file($type, filename => $filename);
is($file->content, $content, "custom file is saved");
is($file->filename, $filename, "valid custom filename");

}

{
my $file = $storage->save($type, $content, filename => $filename);
ok($file->filename eq $filename);
lives_ok { $storage->save('banner_images_uploads', $content, ClientID => 1) } 'same content with other type lives';
}


{
    my $content_1 = 'content 1';
    my $content_2 = 'content 2';

    my $hash_1 = md5_base64ya($content_1);
    my $hash_2 = md5_base64ya($content_2);

    my $f1 = $storage->save('common_file_export', $content_1, filename => $hash_2);
    my $f2 = $storage->save('common_file_export', $content_2, filename => $hash_1);

    my $got_1 = $storage->get_file('common_file_export', filename => $hash_2);
    is($got_1->content, $content_1, 'filename has bigger priority above hash');

    my $got_2 = $storage->get_file('common_file_export', filename => $hash_1);
    is($got_2->content, $content_2);
}


done_testing();

