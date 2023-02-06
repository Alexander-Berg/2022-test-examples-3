package Test::Yandex::MDS;

use strict;
use warnings;
use utf8;

use Carp;
use Test::HTTP::Server;
use Path::Tiny;

=head1 DESCRIPTION

Модуль для тестирования MDS

=head1 SYNOPSYS

use Test::More;
use Test::Yandex::MDS;

my $mds_server = Test::Yandex::MDS->mock_server( namespace => $namespace );

my $mds = Yandex::MDS->new(
    get_host => $mds_server->uri,
    put_host => $mds_server->uri,
    namespace => $namespace,
    ...
);

=cut

my $TMP;
my $AUTH;

sub mock_server
{
    my (%opt) = @_;
    my $ns = $opt{namespace} or croak "namespace required";
    $AUTH = $opt{authorization} or croak "authorization required";

    {
        no strict 'refs';
        *{"Test::HTTP::Server::Request::get-$ns"} = \&_mock_get;
        *{"Test::HTTP::Server::Request::upload-$ns"} = \&_mock_upload;
        *{"Test::HTTP::Server::Request::delete-$ns"} = \&_mock_delete;
    }

    $TMP = Path::Tiny::tempdir('test-yandex-mds-XXXXXXXXXX');

    my $server = Test::HTTP::Server->new();

    return $server;
}

sub _mock_get
{
    my ($req, @key) = @_;
    my $key = join '/', @key;
    unless (path($TMP, $key)->exists) {
        $req->{out_code} = '404 Not Found';
        return '';
    }
    return fetch($key);
}

sub _mock_upload
{
    my ($req, @name) = @_;
    my $data = $req->{body};
    my %headers = @{$req->{headers}};
    my $client_auth = $headers{authorization} // '';
    $client_auth =~ s!^Basic !!;
    unless ($client_auth eq $AUTH) {
        $req->{out_code} = '401 Not Authorized';
        return '';
    }
    my $name = join '/', @name;
    my $key = "42/$name";
    if (path($TMP, $key)->exists) {
        $req->{out_code} = '403 Forbidden';
        return qq#<?xml version="1.0" encoding="utf-8"?>
<post>
<key>$key</key>
</post>
#;
    }
    store($key, $data);
    return qq#<?xml version="1.0" encoding="utf-8"?>
<post id="0:a0b99da07b89...0512d45d6a81" groups="1" size="1" key="$key">
<complete addr="95.108.223.236:1025" path="/srv/storage/19/1/data-0.0" group="1705" status="0"/>
<written>1</written>
</post>#;
}

sub _mock_delete
{
    my ($req, @key) = @_;
    my $key = join '/', @key;
    unless (path($TMP, $key)->exists) {
        $req->{out_code} = '404 Not Found';
        return '';
    }
    unstore($key);
    return '';
}


sub store
{
    my ($key, $val) = @_;
    my $path = path($TMP, $key);
    path($path->parent)->mkpath;
    $path->spew($val);
}

sub fetch
{
    my $key = shift;
    return path($TMP, $key)->slurp();
}

sub unstore
{
    my $key = shift;
    return path($TMP, $key)->remove;
}

1;
