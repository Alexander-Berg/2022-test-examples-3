#!/usr/bin/perl

use strict;
use warnings;
use utf8;

use Test::More;
use Test::Exception;
use Test::Yandex::MDS;
use Direct::Storage;

use Settings;
use Yandex::DBUnitTest qw/:all/;
use Yandex::DBTools;

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

my $storage = Direct::Storage->new(
    get_host => $host,
    put_host => $host,
    authorization => 'hi',
    namespace => 'direct-files',
);

my $content = 'some data';
my $name = 'my_file.txt';

undef local $Yandex::MDS::MDS_GLOBAL_EXPIRE;
my $file1 = $storage->save('common_file_export', $content, filename => $name);
my $file2 = $storage->save('common_file_export', $content, filename => $name.'2');
my $file3 = $storage->save('banner_images_uploads', $content, ClientID => 1);

is($storage->{mds}->get($file3->_mds_key), $content, 'file is in mds');

$storage->delete_file('banner_images_uploads', ClientID => 1, filename => $file3->filename);

is($storage->get_file('banner_images_uploads', filename => $file3->filename, ClientID => 1), undef, "file deleted");
dies_ok { $storage->{mds}->get($file3->_mds_key) } 'file deleted from mds';

is($storage->{mds}->get($file1->_mds_key), $content, 'file is in mds');
$storage->delete_file('common_file_export', filename => $name);

is($storage->{mds}->get($file1->_mds_key), $content, 'file is in mds after delete');


done_testing();

