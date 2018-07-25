#!/bin/bash


nums=$(echo $3 | tr "," "\n")

for i in $nums
do
    


logDir=$1
numP=$2
numV=$i
topic=$4
res=$5


if [ $numP -le "0" ]
then
numP=$numV
fi

resultsPath=~/newResults2/$res/"numV"$numV
echo "Running Validator with "$numV " and "$numP "partitions" 
mkdir -p $resultsPath
utilityPath=./build
osStats=~/osstats

echo "Num V="$numV
echo "log:"$logDir
echo "Topic:"$topic
./stopStartServers.sh 10.0.0.205,10.0.0.115,10.0.0.240,10.0.0.235,10.0.0.119
sleep 20
java -cp .:bin:./libs/*:./kafkaServer/libs/* edu.usc.stalemeter.Utilities $logDir $numP $topic  2>&1 | tee $resultsPath/V$i

sleep 10
/home/mr1/StaleMeter/kafkaServer/bin/kafka-topics.sh --describe --zookeeper 10.0.0.205:2181 --topic $topic 2>&1 | tee -a $resultsPath/V$i

java -Xmx12G -cp /home/mr1/StaleMeter:/home/mr1/StaleMeter/bin:/home/mr1/StaleMeter/libs/*:/home/mr1/StaleMeter/kafkaServer/libs/* edu.usc.stalemeter.ValidationMain -numvalidators $numV -clientid 0 -numclients 1 -numpartitions $numP -app $topic -init false -kafka true -validation true -printfreq 400000 -globalschedule false  -kafkalogdir /home/mr1/StaleMeter/kafkatests 2>&1 | tee -a $resultsPath/V$i



tail -n 1 $resultsPath/V$i >> ~/newResults/$res/allResults.txt

cp $osStats/* $resultsPath/
rm $osStats/*.png
rm $osStats/*.txt

done

