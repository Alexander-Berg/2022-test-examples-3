updater="../../tools/update_key_prefixes"
ya make -r -j10 --checkout --yt-store $updater
updater_bin="${updater}/update_key_prefixes"
$updater_bin --config config.pb.txt --kps key_prefixes.txt
