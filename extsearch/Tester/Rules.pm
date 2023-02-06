package FastRes::Tester::Rules;

use common::sense;

use FastRes;
use FastRes::Creator::Rules;
use FastRes::Tests::Gazetteer;
use FastRes::Tests::Remorphc;

use FastRes::Config;

use Data::Dumper;
use File::Temp qw/tempdir/;

sub new {
    my ($class) = @_;

    return bless {}, ref($class) || $class;
}

sub verify_syntax {
    my ($self, $obj) = @_;

    my $res = FastRes::Creator::Rules::create_obj_rules({object => $obj, is_fast => 1, gzt_for_compile => 1, rules_for_compile => 1});
    if (@{$res->{errors}}) {
        return $res;
    }
    else {
        my $tempdir = tempdir(CLEANUP => 1);

        my $gzttester = FastRes::Tests::Gazetteer->new({
            login   => 'yulika',
            sysname => ($obj->{meta}{system_name} || $obj->{meta}{id}),
            type    => "gazetteer",
            execdir => $tempdir,
        });
        eval {
            $gzttester->test({
                bin_path    => CONFIG->{bin_path},
                src_content => $res->{data}{"gazetteer"},
            });
        };
        if ($@) {
             push @{$res->{errors}}, $@;
             return $res;
        }
        my $err = $gzttester->analyze_errors();
        push @{$res->{errors}}, $err if $err;


        foreach my $d (@{FastRes::DOMAINS()}) {
            next unless $res->{data}{"rules_$d"};

            my $ruletester = FastRes::Tests::Remorphc->new({
                login   => 'yulika',
                sysname => ($obj->{meta}{system_name} || $obj->{meta}{id}),
                type    => "rules_$d",
                execdir => $tempdir,
            });
            eval {
                $ruletester->test({
                    bin_path    => CONFIG->{bin_path},
                    gzt_path    => $gzttester->{dst},
                    src_content => $res->{data}{"rules_$d"},
                });
            };
            if ($@) {
                push @{$res->{errors}}, $@;
                next;
            }
            my $err = $ruletester->analyze_errors();
            push @{$res->{errors}}, $err if $err;
        }

        return $res;
    }
}

1;
