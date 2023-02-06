#!/bin/bash

ARC_ROOT=$1
DIST_ROOT=$2
GRPC_CLIENT_ROOT=$ARC_ROOT/apphost/tools/grpc_client
cd $GRPC_CLIENT_ROOT && ya make -r
mv ./grpc_client $DIST_ROOT
export PATH=$PATH:$DIST_ROOT
