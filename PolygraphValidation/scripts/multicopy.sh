#!/bin/bash


nums=$(echo $3 | tr "," "\n")

for i in $nums
do
    
./stopStartServers.sh 10.0.0.200,10.0.0.205,10.0.0.240,10.0.0.235,10.0.0.105
sleep 20



logDir=$1
numP=$2
numV=$i
topic=$4
res=$5
valid=$6
if [ $numP -le "0" ]
then
numP=$numV
fi

echo "Running Validator with "$i " and "$numP " partitions"


resultsPath=~/newResults/$res/"numV"$numV
mkdir -p $resultsPath
utilityPath=./build
osStats=~/osstats
if [ "$numV" -ge "2" ]
then
dividedV=$(expr $numV / 2)
else 
dividedV="1"
fi
echo "Num V="$numV
echo "divided V="$dividedV
echo "log:"$logDir
echo "Topic:"$topic
sleep 10
java -cp .:bin:./libs/*:./kafkaServer/libs/* edu.usc.stalemeter.Utilities $logDir $numP $topic  2>&1 | tee $resultsPath/V$i

sleep 10
/home/mr1/StaleMeter/kafkaServer/bin/kafka-topics.sh --describe --zookeeper 10.0.0.205:2181 --topic $topic 2>&1 | tee -a $resultsPath/V$i

if [ "$numV" -ge "2" ]
then
ssh 10.0.0.210 "mkdir -p $resultsPath"
echo "starting validator at 10.0.0.210 with "$dividedV
ssh -n -f 10.0.0.210 "rm /home/mr1/screenlog.0"
ssh -n -f 10.0.0.210 screen -L -S validator -dm "java -Xmx12G -cp /home/mr1/StaleMeter:/home/mr1/StaleMeter/bin:/home/mr1/StaleMeter/libs/*:/home/mr1/StaleMeter/kafkaServer/libs/* edu.usc.stalemeter.ValidationMain -numvalidators $numV -numpartitions $numP -clientid 1 -numclients 2 -app $topic -init false -kafka true -validation $valid -printfreq 400000 -globalschedule false -kafkaosstats false -kafkalogdir /home/mr1/StaleMeter/kafkatests"

fi

echo "starting validator at 10.0.0.120 with "$dividedV

java -Xmx12G -cp /home/mr1/StaleMeter:/home/mr1/StaleMeter/bin:/home/mr1/StaleMeter/libs/*:/home/mr1/StaleMeter/kafkaServer/libs/* edu.usc.stalemeter.ValidationMain -numvalidators $numV -numpartitions $numP -clientid 0 -numclients 2 -app $topic -init false -kafka true -kafkaosstats false -validation $valid -printfreq 400000 -globalschedule false -kafkalogdir /home/mr1/StaleMeter/kafkatests 2>&1 | tee -a $resultsPath/V$i


if [ "$numV" -ge "2" ]
then
while ssh 10.0.0.210 "screen -list | grep -q validator"
do 
sleep 60
echo "waiting"
done
fi



tail -n 1 $resultsPath/V$i >> ~/newResults/$res/allResults.txt

if [ "$numV" -ge "2" ]
then
ssh -n -f 10.0.0.210 "cat screenlog.0 >$resultsPath/V$i"
ssh -n -f 10.0.0.210 "rm /home/mr1/screenlog.0"
ssh -n -f 10.0.0.210 "tail -n 1 $resultsPath/V$i >> ~/newResults/$res/allResults.txt"
ssh -n -f 10.0.0.210 "cp $osStats/* $resultsPath/"
ssh -n -f 10.0.0.210 "rm $osStats/*.png"
ssh -n -f 10.0.0.210 "rm $osStats/*.txt"
mkdir $resultsPath/210
scp -r 10.0.0.210:$resultsPath/ $resultsPath/210
scp -r 10.0.0.210:~/newResults/$res/allResults.txt $resultsPath/210/
fi


cp $osStats/* $resultsPath/
rm $osStats/*.png
rm $osStats/*.txt

done
