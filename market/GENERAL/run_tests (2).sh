echo '===='
echo 'LOCAL RUN replenishment graph_generator'
echo '===='
if [ -z "${ARC_HOME}" ]; then
  arcadia_home=$HOME/arc/arcadia
else
  arcadia_home="${ARC_HOME}"
fi
echo "* arcadia_home = $arcadia_home"
if [ ! -d $arcadia_home ]
then
    echo "arcadia должна быть примонтирована в `echo $arcadia_home`"
    echo "создайте ссылку существующего арка на ~/arc или примонтируйте арк заново"
    exit 0001
fi
repl_home=$arcadia_home/market/replenishment/algorithms
echo "* build $repl_home/bin"
echo "* /common_calculator"
$arcadia_home/ya make -tt --yt-store -DHAVE_CUDA=no $repl_home/bin/common_calculator || exit
echo "* /common_calculator_v2"
$arcadia_home/ya make -tt --yt-store -DHAVE_CUDA=no $repl_home/bin/common_calculator_v2 || exit
echo "* /vicugna"
$arcadia_home/ya make -tt --yt-store -DHAVE_CUDA=no $repl_home/bin/vicugna || exit
echo "* build $repl_home/bin/graph_generator"
$arcadia_home/ya make -tt --yt-store -DHAVE_CUDA=no $repl_home/bin/graph_generator || exit
