#!/usr/bin/env bash

set -e

CLASS=com.dynamo.bob.pipeline.Spine
JAR=./defold-simpledata/plugins/share/pluginSimpleDataExt.jar
BOB=~/work/defold/tmp/dynamo_home/share/java/bob.jar

java -cp $JAR:$BOB:./defold-simpledata/plugins/lib/x86_64-osx $CLASS $*
