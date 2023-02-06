#!/usr/bin/perl

# $Id$

=pod

    Проверка, что на всех скриптах выставлено свойство svn:executable

=cut

# TODO 
#  * проверять в деплоях и shebang тоже, см. unit_tests/perl/shebang.t
# ??? Надо ли проверять shebang не только в perl'овых, но и в shell'ных скриптах?
#  * проверять другие svn-свойства (не только на скриптах): svn:keywords, svn:eol-style, 
#    возможно -- svn:mime-type на файлах с "бинарными" расширениями и на очень больших (=генерируемых, нечеловекочитаемых)

use warnings;
use strict;

use Test::More;

use XML::LibXML;
use File::Basename qw/basename/;

use Test::ListFiles;

use Settings;

#..........................................................................................................
my $ROOT = $Settings::ROOT;
my $SCRIPTS_ROOTS = ["$Settings::ROOT"];

my @SCRIPT_FILE_REGEXP = (
    '\.pl',
    '\.sh',
);

# Список исключений. В идеале пополняться не должен.
my @KNOWN_SCRIPTS_WO_EXECUTABLE = (
    '20100309_rbac_superplacers_allow_show_accesslogs.sh',
    '20100604_update_domain_logins.pl',
    '20100722_update_api_users_units.pl.pl',
);

my $CORRECT_EXECUTABLE_VALUE_REGEXP = '\*';

#..........................................................................................................

my $script_filename_regexp = "([^/]+(?:".join("|", @SCRIPT_FILE_REGEXP)."))";
my $known_scripts_wo_executable_regexp = "^(?:".join("|", @KNOWN_SCRIPTS_WO_EXECUTABLE).")\$";

#..........................................................................................................

my %SCRIPT_EXISTS;
for my $file (grep {-f && m/$script_filename_regexp$/ } Test::ListFiles->list_repository($SCRIPTS_ROOTS)) {
    my $basename = basename($file);
    next if $basename =~ /$known_scripts_wo_executable_regexp/;
    $SCRIPT_EXISTS{$file} = 1;
}

#..........................................................................................................

Test::More::plan(tests => scalar(keys %SCRIPT_EXISTS));

my $xml = `svn propget -R --xml svn:executable $ROOT 2>&1`;

SKIP: {
if ($xml =~ /not a working copy/) {
    skip "only svn working copy is supported", scalar(keys %SCRIPT_EXISTS);
}

my $doc = XML::LibXML->new()->parse_string($xml)->documentElement();            
my @targets = $doc->findnodes("/properties/target");            

my %EXECUTABLE;
for my $t (@targets) {
    my $path = $t->getAttribute('path');
    my $value = $t->findvalue("./property/text()");

    $EXECUTABLE{$path} = $value;
}

#..........................................................................................................

for my $file (keys %SCRIPT_EXISTS){
    my $ok = $EXECUTABLE{$file} =~ $CORRECT_EXECUTABLE_VALUE_REGEXP;
    ok($ok, "file $file should have an svn:executable property");
}

}
