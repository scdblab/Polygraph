#!/bin/bash
logDir=$1
numV=$2
topic=$3

resultsPath=~/results/$4/"numV"$numV
mkdir -p $resultsPath
utilityPath=./build
osStats=~/osstats
#rm $osStats/*


echo "Num V="$numV
echo "log:"$logDir
echo "Topic:"$topic
java -cp .:bin:./libs/*:./libs/kafka/* edu.usc.stalemeter.Utilities $logDir $numV $topic
sleep 1
let " m = 12288 / $numV "
echo "Starting Validators"
for ((i=0; i<numV; i++)); do
   echo "$i"
xmx="-Xmx"$m"m"
echo "xmx = $xmx"
java $xmx -cp .:bin:./libs/*:./libs/kafka/* edu.usc.stalemeter.ValidationMain -id $i -numvalidators $numV -app $topic -init true -kafka true -printfreq 1000000 -globalschedule false -er ./props -kafkalogdir ./kafkatests 2>&1 | tee $resultsPath/V$i &



done
wait 


for ((i=0; i<numV; i++)); do
tail -n 1 $resultsPath/V$i >> $resultsPath/allResults.txt
done


cp $osStats/* $resultsPath/
rm $osStats/*.png
rm $osStats/*.txt
echo "Done"



