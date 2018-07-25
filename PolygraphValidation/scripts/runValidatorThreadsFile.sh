#!/bin/bash
for logDir in $1/* ; do


numV=1

fileName=$(basename $logDir)
topic="BG"
if [[ $fileName == *"TPCC"* ]]
then
  topic="TPCC";
fi
echo "file:"$fileName
echo "Topic:"$topic

resultsPath=~/results/$2/"file-"$fileName
mkdir -p $resultsPath

osStats=~/osstats






java -Xmx12G -cp .:bin:./libs/*:./libs/kafka/* edu.usc.stalemeter.ValidationMain -numvalidators $numV -app $topic -init true -kafka false -printfreq 1 -globalschedule false -filelogdir $logDir 2>&1 | tee $resultsPath/$fileName

tail -n 1 $resultsPath/$fileName >> ~/results/$2/allResults.txt

cp $osStats/* $resultsPath/
rm $osStats/*.png
rm $osStats/*.txt

done

