#!/usr/bin/perl

# пострелять патронами из файла без танка.
# формат файла с патронами
# https://yandextank.readthedocs.io/en/latest/tutorial.html#request-style
# пример
# https://storage-int.mds.yandex.net/get-load-ammo/29344/f5200ef985ae4cc48ed9ddb24c4807cd
# файл читается из stdin
# никакой проверки корректности формата не делается.

use strict;
use warnings;
use utf8;
use open qw/:std :encoding(UTF-8)/;

use IO::Socket::SSL qw/SSL_VERIFY_NONE/;
use Encode qw/encode_utf8 decode_utf8/;

while (1) {
    my $len = "";
    my $chars_read = 1;
    my $offset = 0;
    while ($len !~ /\n$/ && $chars_read != 0) { $chars_read = read \*STDIN, $len, 1, $offset++ };
    last if $chars_read == 0;
    chomp $len;
    warn "got request length $len\n";
    my $req;
    read \*STDIN, $req, $len + 1;
    warn "request: $req";
    my $socket = IO::Socket::SSL->new(PeerHost => "ppctest-ts3-front.ppc.yandex.ru", PeerPort => "14443", SSL_verify_mode => SSL_VERIFY_NONE) or die "cannot create socket: $@";
#    my $socket = IO::Socket::SSL->new(PeerHost => "test-direct.yandex.ru", PeerPort => "14443", SSL_verify_mode => SSL_VERIFY_NONE) or die "cannot create socket: $@";
    $socket->print("$req\n");
    while (<$socket>) { print STDERR decode_utf8($_) };
#    warn "sleeping 5 secs\n";
#    sleep 5;
}
