#!/usr/bin/perl

use warnings;
use strict;
use File::Slurp;
use Test::More;

use Test::ListFiles;
use Yandex::ListUtils qw/xsort/;
use Settings;

my $ROOT = "$Settings::ROOT/protected";

my %SKIP_SUBS = map {$_ => 1} (
    qw/DESTROY BUILD FLUSH/,

    qw/
    generate_iterator
    get_priorities_hash
    get_all_geo_ids
    get_record_dates
    _get_topics_hash
    _get_encodings
    _get_regions
    as_json
    unlock_wallet
    get_hits_by_cost_context_table
    delete_user_custom_options
    has_bid_modifiers_in_java_allowed_feature
    set_cookie
    get_major_os_version

    is_imagead_supported
    is_disabled_domains_changed
    is_is_different_places_changed
    is_disabled_ips_changed
    is_competitors_domains_changed
    is_parse_results_changed

    is_disabled_video_placements_changed

    get_groups_params
    additions_item_type
    child_has_status_moderate
    has_flags

    is_universal_campaign_client
    _get_countable_fields_dependencies
    _get_all_countable_fields
    /,
);

my %SKIP_SUBS_FROM = map {$_ => 1} qw!
    protected/Sandbox/Balance.pm
    protected/Sandbox/Balance2.pm
    protected/Template/Plugin/DocsL10n.pm
    protected/Template/Plugin/Sitelinks.pm
    protected/Direct/Monitor/Daily.pm
    protected/Direct/Monitor/Base.pm
    protected/Apache/StartUp.pm
    protected/Direct/PredefineVars.pm
    protected/Direct/Test/DBObjects.pm
    protected/Test/CreateDBObjects.pm
    protected/RBAC2/Extended.pm
    protected/ServiceSOAP.pm
    protected/Yandex/DBQueue/Job.pm
    protected/Yandex/Subscriptions.pm
    protected/StatXLS.pm
    protected/Yandex/CallerStack.pm
    protected/DevTools.pm
    protected/Intapi/FakeAdmin.pm
    protected/Units.pm
    protected/BS/Export/SOAPSerializer.pm
    protected/YaCatalogApi.pm
    protected/Yandex/ORM/Model/Base.pm
    protected/Yandex/Test/ValidationResult.pm
    protected/Yandex/Test/Tools.pm
    protected/Test/Subtest.pm
    protected/Test/ListFiles.pm
    protected/RedisLock.pm
    protected/Direct/ValidationResult.pm
    protected/Direct/Defect.pm
    protected/ShardingTools.pm

    protected/RBACDirectOld.pm
    perl/rbac-elementary/RBACElementaryOld.pm

    api/lib/API/Service/ResultSet/Base.pm
    api/lib/API/Service/ResultSet/Item.pm
    api/lib/API/Services.pm
    api/lib/WSDL/JSON/Schema/Sequence.pm
    api/services/v5/API/Service/Base.pm

    protected/maintenance/api_test.pl
    protected/maintenance/api_client_json.pl
!;

my @perl_files = 
    map {+{name => $_, rel_name => s/^\Q$Settings::ROOT\E\/*//r, mod => mod_name($_)}}
    grep {-f && /\.(pm|pl|t)$/} 
    Test::ListFiles->list_repository($Settings::ROOT);

sub mod_name {
    my $file = shift;
    return undef unless $file =~ s/\.pm$//;
    $file =~ s/^\Q$Settings::ROOT\E\/*(protected|unit_tests|perl\/\w+|api\/lib|api\/services\/v5)\/+// || return undef;
    $file =~ s/\//::/g;
    return $file;
}

my %UNUSED_MODULES = map {$_->{mod} => 1} grep {$_->{mod}} @perl_files;
my @subs;
my $all_cont;
for my $file (@perl_files) {
    my $cont = scalar read_file $file->{name};
    $cont =~ s/\n=\w+.*?\n=cut//gs;
    $cont =~ s/^\s*#.*//gm;
    $cont =~ s/\n__(END|DATA)__\s*\n.*//s;
    $cont =~ s!\@EXPORT(_OK)?\s*=\s*qw[/\(][a-z0-9_\s\$\@\%\&]+[/\)]!!gi;
    $cont =~ s/((?:Yandex::Trace|\$log|\b_?log_).+)(['"])[_a-z0-9:-]+\2/$1$2 HERE_WAS_LITERAL $2/g;
    for my $dep ($cont =~ /(?:;|^|\n)\s*(?:use|require)\s+([a-z0-9_:]+)/gi) {
        delete $UNUSED_MODULES{$dep};
    }
    if ($file->{rel_name} !~ /unit_tests|one-shot/) {
        while($cont =~ s/^\s*sub\s+(\w+)/ HERE_WAS_SUB /m) {
            my $sub_name = $1;
            unless (
                    $file->{rel_name} =~ /DoCmd/ && $sub_name =~ /^cmd_/
                    || $file->{rel_name} =~ /DostupUserManagement/ && $sub_name =~ /^cmd_/
                    || $file->{rel_name} =~ /FormCheck/ && $sub_name =~ /^vld_/
                    || $file->{rel_name} =~ /PSGI/ && $sub_name =~ /^get_app$/
                    || $file->{rel_name} =~ /^deploy\//
                    || $file->{rel_name} =~ /Fake/
                    || $sub_name =~ /^_build_/
                    || exists $SKIP_SUBS_FROM{$file->{rel_name}}
                    || exists $SKIP_SUBS{$sub_name}
            ) {
                push @subs, {name => $sub_name, file => $file->{rel_name}};
            }
        }
    }
    if ($file->{rel_name} !~ /unit_tests/) {
        $all_cont .= $cont."\n";
    }
}

for my $subr (xsort {$_->{file}} @subs) {
    my $sub_name = $subr->{name};
    my $type = $subr->{file} =~ /^(api|protected\/(API))/ ? "api" 
        : $subr->{file} =~ /^protected\/(Direct|Model)/ ? "core"
        : $subr->{file} =~ /^protected\/(BS|Moderate|Stat)\// ? "transport"
        : "other";
    if ($ENV{PRINT_UNUSED}) {
        unless ($all_cont =~ /\b\Q$sub_name\E\b/) {
            print "Subroutine $sub_name in $subr->{file} (type=$type) is unused!\n";
        }
    } else {
        ok $all_cont =~ /\b\Q$sub_name\E\b/,
            "Subroutine $sub_name in $subr->{file} (type=$type) is unused!";
    }
}


done_testing;
