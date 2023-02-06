#!/usr/bin/perl

use strict;
use warnings;

use File::Slurp;
use Test::More;
use File::Basename;

use Test::ListFiles;
use Settings;

use Yandex::Test::UTF8Builder;
use utf8;

my $PATH_ROOT = "$Settings::ROOT/data/";

my %SKIP_FILES = map {$_ => 1} qw/
    highcharts.src.js
/;
# TODO bem/techs/bemhtml.js -- подозрительно
my $SKIP_FILES_RE = join "|", qw(
data/lego/blocks/b-form-radio/examples/70api.js 
data/lego/blocks/i-bem/html/bem/techs/bemhtml.js 
data/lego/blocks/i-bem/html/tests/tests.js
),
# приехали с переносом externals: DIRECT-94510
qw(
data/lego/tools/bem/techs/nodejs/bemhtml.js
data/lego/tools/bem/techs/nodejs/deps.js.js
data/lego/tools/bemjson2bemdecl.js
data/lego/tools/bemjson2html.js
data/lego/tools/eval/blocks/b-js-example/b-js-example.js
data/lego/tools/make-block-decl.js
data/lego/tools/make-block-wiki.js
data/lego/tools/prettybemjson.js
data/lego/tools/util.js
);

# ищем все файлы, в которых может встретиться javascript
my @files = sort
    grep {!$SKIP_FILES{basename($_)}}
    grep {$_ !~ /$SKIP_FILES_RE$/}
    grep {-f && /\.(?:html|t|js|htm|tt2)$/i}
    Test::ListFiles->list_repository($PATH_ROOT);

Test::More::plan(tests => 2*scalar(@files));

for my $file (@files) {
    my $template = read_file($file);

    # удаляем комменатрии
    $template =~ s#^\s*//.*?$/##gm;
    $template =~ s#/\*(?:.|[\r\n])*?\*/##g;

    ok($template !~ /\bconsole[\._]log\b/, "Checking $file: firebug's debug print (console.log)");
    ok($template !~ /\bdebugger\b/, "Checking $file: debugger() function call");
}
