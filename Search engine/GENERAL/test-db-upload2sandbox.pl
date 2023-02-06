#!/usr/bin/perl
use strict;
use warnings;
use POSIX;
use Data::Dumper;

use RPC::XML qw();
use RPC::XML::Client qw();

unless (@ARGV == 1) {
    warn "Usage: $0 (cleanup|newdb)";
    warn "Report cleanup: $0 cleanup";
    warn "Report newdb: bsconfig ilookup itag=newdb shard=/.../ --twocolumn --batch-mode | $0 newdb";
    exit(1);
}

my $cli = RPC::XML::Client->new('https://sandbox.yandex-team.ru/sandbox/xmlrpc');

my $req;
if ($ARGV[0] eq 'cleanup') {
    $req = RPC::XML::request->new("cleanupBaseTesting", "Cleanup at ".localtime(time));
    print "DEBUG: reported cleanup.\n";
} elsif ($ARGV[0] eq 'newdb') {
    my $data = []; # [ [shard, host] , ... ]
    while(<STDIN>) {
        chomp;
        if (/(\S+)\s([^:]+):/) {
            push(@$data, [ $1, $2 ]);
        } else {
            warn "Bad input line $_";
        }
    }
    $req = RPC::XML::request->new("baseOnHostsReady", $data);
    print "DEBUG: reported ",scalar(@$data)," pairs.\n";
}
# proxy = xmlrpclib.ServerProxy('https://sandbox.yandex.ru/sandbox/xmlrpc')
# proxy.cleanupBaseTesting("Base name")
# proxy.baseOnHostsReady(['ws4-296', 'ws3-049', 'ws5-614', 'ws5-324', 'ws5-028', 'ws7-159', 'ws8-108', 'ws6-370', 'ws7-318', 'ws6-069'])
# ...

my $res=$cli->send_request($req);
if (not ref($res)) {
    die "Internal error on send : ",$res; 
}
print "DEBUG: reported  : ", Dumper($res),"\n";
if ( $res->is_fault ) {
    if( $res->string ) {
        warn "Failed : status(", $res->code,") body(",$res->string,")";
    } else {
        warn "Failed : status(", $res->code,")";
    }
} else{
    print "DEBUG: reported  : ", Dumper($res->value),"\n";
}
exit(0);
