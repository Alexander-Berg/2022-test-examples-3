#!/usr/bin/perl -w
use strict;
use warnings;

use URI::Escape;
use MIME::Base64;

use lib '/etc/nginx/YandexDisk';
use DownloaderCipher;


my $in_stid = $ARGV[0] or die "Please specify encrypted stid";

if ($in_stid =~ m,(?:https?://[\w\-\.]+/)?r?(?:disk|int_get_private_file|share|preview|zip)/\w+/\w+/(.*?)\?,) {
    $in_stid = $1;
    print "* Link found, extracted stid:\n$in_stid\n";
}

my $stid = uri_unescape $in_stid;
$stid =~ tr|-_|+/|;

my $decodedBase64 = decode_base64($stid);
if ($decodedBase64 =~ /^[\w\.\:]+$/) {
    print "* Plain (BASE64 only) stid:\n";
    $stid = $decodedBase64;
} else {
    print "* Decrypted (AES+BASE64) stid:\n";
    $stid = YandexDisk::decrypt($stid);
}

print "$stid\n";

exit 0;

