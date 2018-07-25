#!/bin/bash



kafkaPath=/home/mr1/StaleMeter/kafkaServer/



export KAFKA_HEAP_OPTS="-Xmx10G"

#export KAFKA_JVM_PERFORMANCE_OPTS="-XX:MetaspaceSize=96m -XX:+UseG1GC -XX:MaxGCPauseMillis=20 -XX:InitiatingHeapOccupancyPercent=35 -XX:G1HeapRegionSize=16M -XX:MinMetaspaceFreeRatio=50 -XX:MaxMetaspaceFreeRatio=80"



cd $kafkaPath


echo ""
echo "======================================================================"
echo "======================== STARTING KAFKA =============================="
echo "======================================================================"

nohup bin/kafka-server-start.sh config/server.properties&

sleep 5

echo "======================================================================"
echo "======================== STARTING KAFKA END =========================="
echo "======================================================================"
    
echo "Done2"







