#!/usr/bin/env bash
ulimit -c unlimited
set -xe
set -o pipefail

b="./mvel-mixedtype-io"

ya make -j3

# make sample
cat >bla<<EOF
bla bla bla
bla     bla
bla bla bla
EOF
cat bla | gzip > bla.gz

# test files
$b "bla" de "bla.out" de && diff bla bla.out
$b "bla.gz" gz "bla.out" de && diff bla bla.out
$b "bla" de "bla.out.gz" gz && zcat bla.out.gz > bla.out.de && diff bla bla.out.de
$b "bla.gz" gz "bla.out.gz" gz && zcat bla.out.gz > bla.out.de && diff bla bla.out.de

cat "bla" | $b "-" de "-" de > bla.out && diff bla bla.out
cat "bla.gz" | $b "-" gz "-" de > bla.out && diff bla bla.out
cat "bla" | $b "-" de "-" gz > bla.out.gz && zcat bla.out.gz > bla.out.de && diff bla bla.out.de
cat "bla.gz" | $b "-" gz "-" gz > bla.out.gz && zcat bla.out.gz > bla.out.de && diff bla bla.out.de

rm bla*
