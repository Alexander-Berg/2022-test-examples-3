set -eoux pipefail

generation=$1
echo $generation
fake_generation=20190618_1027
test_data_dir=test_data
dst_dir=${test_data_dir}/${fake_generation}

mkdir -p ${dst_dir}
chmod a+w test_data -R || :

rsync --archive --verbose --progress  -r --inplace --files-from=files logview.market.yandex.net:/mnt/remote-log-rfs/ps01ht.market.yandex.net/var-lib-yandex/indexer/market/${generation} ${dst_dir}
