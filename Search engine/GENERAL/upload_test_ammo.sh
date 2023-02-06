# Uploading test ammo (resource type=OTHER_RESOURCES)
gzip -c $1 > "$1.gzip"
ya upload "$1.gzip"
