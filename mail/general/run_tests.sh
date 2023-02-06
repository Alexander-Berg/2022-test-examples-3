set -ex

printenv

cd $WORKSPACE

arc token store || true

mkdir -p arcadia store

arc_shared_store=${HOME}/.delivery/arc_store
if [ ! -d $arc_shared_store ]; then
    mkdir -p $arc_shared_store
fi

arc mount -m arcadia/ -S store/ --object-store ${arc_shared_store}

cd arcadia

if [ "$REVISION" != "HEAD" ]; then
    arc checkout "r$REVISION" &> /dev/null
fi

if [ -n "$REVIEW_ID" ]; then
    arc pr checkout $REVIEW_ID &> /dev/null
fi

cd mail/$PROJECT

result=""
build_dir_debug="--build-dir=${HOME}/.ya/build_debug/"

ya make $build_dir_debug

if [ $RUN_SMALL = "true" ]; then
    ya_make_key="-A --test-size=small $build_dir_debug"
    ya make $ya_make_key
    if [ $? -eq 0 ]; then
        result+="Tests small ($ya_make_key) !!(green)success!!\n"
    else
        result+="Tests small ($ya_make_key) !!(red)failed!!\n"
    fi
fi

if [ $RUN_MEDIUM = "true" ]; then
    ya_make_key="-A --test-size=medium $build_dir_debug"
    ya make $ya_make_key
    if [ $? -eq 0 ]; then
        result+="Tests medium ($ya_make_key) !!(green)success!!\n"
    else
        result+="Tests medium ($ya_make_key) !!(red)failed!!\n"
    fi
fi

if [ $RUN_LARGE = "true" ]; then
    if [ -z "$RUN_LARGE_TEST_TAGS" ]; then
        ya_make_key="-A --test-size=large $build_dir_debug"
        ya make $ya_make_key
        if [ $? -eq 0 ]; then
            result+="Tests large ($ya_make_key) !!(green)success!!\n"
        else
            result+="Tests large ($ya_make_key) !!(red)failed!!\n"
        fi
    else
        IFS=' ' read -a keys <<< "$RUN_LARGE_TEST_TAGS"
        for key in ${keys[@]}; do
            ya_make_key="-A --test-tag $key $build_dir_debug"
            ya make $ya_make_key
            if [ $? -eq 0 ]; then
                result+="Tests large ($ya_make_key) !!(green)success!!\n"
            else
                result+="Tests large ($ya_make_key) !!(red)failed!!\n"
            fi
        done
    fi
fi

echo "RESULT=${result}" >> $WORKSPACE/result

cd $WORKSPACE

mount | grep arc | awk '{print "arc unmount "$3}' | bash || true
