package FastRes::Tests::Common;

use common::sense;

use File::Temp qw/tempdir/;

use FastRes::Util qw/run/;

# static: suffix, dotbin = '.bin'
# params: login, type, sysname (?),

my %defdata = (
    login       => 'temp',
    type        => 'def',
    sysname     => 'def',
    src_suffix  => '.tmp',
    dst_suffix  => '.bin',
);

sub new {
    my ($class, $params) = @_;
    my $self = bless {}, $class;

    $self->init($params);

    return $self;
}

sub _init {
    my ($self, $params) = @_;
    my %data = (%defdata, %$params);

    $self->{execdir} = $data{execdir} || tempdir(CLEANUP => 1);
    $self->{src_name} = join("_", @data{qw/login sysname type/}).".".time().$data{src_suffix};
    $self->{dst_name} = $self->{src_name}.$data{dst_suffix};

    $self->{src} = $self->{execdir} ."/". $self->{src_name};
    $self->{dst} = $self->{execdir} ."/". $self->{dst_name};
}

sub test {
    my ($self, $data) = @_;

    $self->_prepare($data);
    $self->_make_command($data);
    return $self->_exec_command();
}

sub _prepare {
    my ($self, $data) = @_;
    if ($data->{src_content}) {
        open FH, ">:utf8", $self->{src} or die "Cannot open file for writing source data: $@";
        print FH $data->{src_content};
        close FH;
    }
}

#Stub
sub _make_command {
    my $self = shift;
    $self->{command} = 'echo 1';
}

sub _exec_command {
    my $self = shift;
    die "Command for test is not defined!" unless $self->{command};
    $self->{result} = run($self->{command});
}

sub analyze_errors {
    return "Error: used 'analyze_errors' from FastRes::Tests::Common";
}

1;
