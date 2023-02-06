#!/bin/bash

tar -cvzf tutor_configs.tar.gz config_files
ya upload tutor_configs.tar.gz -d "Tutor configs" --ttl inf --token $1
