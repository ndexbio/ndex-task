#!/bin/bash
export JAVA_HOME=/usr/lib/jvm/java-7-oracle
echo "Java Home is $JAVA_HOME"
export CLASSPATH=.:..:$CLASSPATH:
echo "Path is is $PATH"
echo "CLASSPATH  is $CLASSPATH"
$JAVA_HOME/bin/java -jar /opt/ndex/scripts/ndex-task-0.0.1-SNAPSHOT-jar-with-dependencies.jar
