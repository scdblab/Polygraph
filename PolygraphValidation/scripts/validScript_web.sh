#!/bin/bash

cd /home/mr1/StaleMeter/

numV=$2
topic=$1
ip=$4
jsonFile=$5
resultsPath=$3/results
mkdir -p $resultsPath
utilityPath=./build
osStats=~/osstats
#rm $osStats/*

let " m = 12288 / $numV "

for ((i=0; i<numV; i++)); do

xmx="-Xmx"$m"m"
java $xmx -cp .:bin:./libs/*:./kafkaServer/libs/* edu.usc.stalemeter.ValidationMain -id $i -numvalidators $numV -app $topic -init false -kafka true -printfreq 1000000 -globalschedule false -continuous true -kafkahost $ip -er $jsonFile -kafkalogdir ./kafkatests 2>&1 | tee $resultsPath/V$i &

done
