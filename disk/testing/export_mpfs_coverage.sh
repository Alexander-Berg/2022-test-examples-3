#!/bin/bash
# Актуализируем версию MPFS
MPFS_VERSION=$(dpkg-parsechangelog -lapps/disk/deploy/debian/changelog --show-field Version)
sed -i 's/sonar.projectVersion=1.0/sonar.projectVersion='$MPFS_VERSION'/g' mpfs-sonar-project.properties

/opt/sonar/sonar-scanner-2.8/bin/sonar-scanner -Dsonar.projectKey=Disk::MPFS::MPFS -Dsonar.sources=lib/mpfs -Dproject.settings=mpfs-sonar-project.properties

# Игронируем результат экспорта, чтобы не аффектить тесты
exit 0
