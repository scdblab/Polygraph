#!/bin/bash

cd /home/mr1/StaleMeter

#./runValidatorThreadsMultiServers.sh /home/mr1/BG-10K-100fpm-100Threads-60Min-part-noStale-uniform -1 64,48,32,16,8,4,2,1 BG paper-bg-uniform-1m true 10.0.0.120
#./runValidatorThreadsMultiServers.sh /home/mr1/BG-10K-100fpm-100Threads-60Min-part-noStale -1 1,2,4,64,48,32,16,8 MR1 paper-bg-skewed-1m_3 true 10.0.0.107
./runValidatorThreadsMultiServers.sh /home/mr1/TPCC_100W_300T_3Hours_15MinWarmup -1 16,32,8,4,2 TPCC paper-tpcc-1m_FTE true 10.0.0.120

#./runValidatorThreadsMultiServers.sh /home/mr1/BG-10K-100fpm-100Threads-60Min-part-noStale-uniform -1 64,48,32,16,8,4,2 MR1 paper-bg-uniform-2m true 10.0.0.120,10.0.0.107
#./runValidatorThreadsMultiServers.sh /home/mr1/BG-10K-100fpm-100Threads-60Min-part-noStale -1 64,48,32,16,8,4,2 MR1 paper-bg-skewed-2m true 10.0.0.120,10.0.0.107
./runValidatorThreadsMultiServers.sh /home/mr1/TPCC_100W_300T_3Hours_15MinWarmup -1 32,16,8,4,2 TPCC paper-tpcc-2m_FTE true 10.0.0.120,10.0.0.107

