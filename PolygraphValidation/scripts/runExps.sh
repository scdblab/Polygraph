#!/bin/bash



cd /home/mr1/StaleMeter

nums=1

for i in $nums
do


#./runValidatorThreads.sh /home/mr1/TPCC_100W_300T_3Hours_15MinWarmup/ -1 64,48,32,16,8,4,2,1 TPCC tpcc-2-1m-final_$i true
#./runValidatorThreadsMulti.sh /home/mr1/TPCC_100W_300T_3Hours_15MinWarmup/ -1 64,48,32,16,8,4,2,1 TPCC tpcc-2-2m-final_$i true

#./runValidatorThreads.sh /home/mr1/BG-10K-100fpm-100Threads-60Min-part-noStale/ -1 64,48,32,16,8,4,2,1 MR1 bg-skew-1m-final_$i true
#./runValidatorThreadsMulti.sh /home/mr1/BG-10K-100fpm-100Threads-60Min-part-noStale/ -1 64,48,32,16,8,4,2,1 MR1 bg-skew-2m-final_$i true

#./runValidatorThreads.sh /home/mr1/BG-10K-100fpm-100Threads-60Min-part-noStale-uniform/ -1 64,48,32,16,8,4,2,1 MR1 bg-uniform-1m-final_$i true
#./runValidatorThreadsMulti.sh /home/mr1/BG-10K-100fpm-100Threads-60Min-part-noStale-uniform/ -1 64,48,32,16,8,4,2,1 MR1 bg-uniform-2m-final_$i true
./runValidatorThreadsMulti.sh /home/mr1/BG-10K-100fpm-100Threads-60Min-part-noStale/ -1 64,48,32,16,8,4,2,1 MR1 bg-skew-2m-final3_$i true





done






