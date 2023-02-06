package FastRes::Tester::Requests;

use common::sense;

use FastRes;
use FastRes::Creator::Requests;
use FastRes::Tests::Gazetteer;

use FastRes::Config;

use Data::Dumper;
use File::Temp qw/tempdir/;

sub new {
    my ($class) = @_;

    return bless {}, ref($class) || $class;
}

sub verify_syntax {
    my ($self, $obj) = @_;

    my $res = FastRes::Creator::Requests::create_object_data({object => $obj, gzt_for_compile => 1});
    if (@{$res->{errors}}) {
        return $res;
    }
    else {
        my $tempdir = tempdir();#CLEANUP => 1);

        my $gzttester = FastRes::Tests::Gazetteer->new({
            login   => 'yulika',
            sysname => ($obj->{meta}{system_name} || $obj->{meta}{id}),
            type    => "gazetteer",
            execdir => $tempdir,
        });
        eval {
            $gzttester->test({
                bin_path    => CONFIG->{bin_path},
                src_content => $res->{data}{gazetteer},
            });
        };
        if ($@) {
             push @{$res->{errors}}, $@;
             return $res;
        }
        my $err = $gzttester->analyze_errors();
        push @{$res->{errors}}, $err if $err;

        return $res;
    }
}

1;
