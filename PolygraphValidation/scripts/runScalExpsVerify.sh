#!/bin/bash



cd /home/mr1/StaleMeter




 ./runValidatorThreadsMultiServers.sh /home/mr1/BG-10K-100fpm-100Threads-60Min-part-noStale 211 48,32,16,8,4,2,1 MR1 BG-skewed-200-1 true 10.0.0.120

 ./runValidatorThreadsMultiServers.sh /home/mr1/BG-10K-100fpm-100Threads-60Min-part-noStale 211 48,32,16,8,4,2,1 MR1 BG-skewed-200-2 true 10.0.0.120

 ./runValidatorThreadsMultiServers.sh /home/mr1/BG-10K-100fpm-100Threads-60Min-part-noStale 211 48,32,16,8,4,2,1 MR1 BG-skewed-200-3 true 10.0.0.120

 ./runValidatorThreadsMultiServers.sh /home/mr1/BG-10K-100fpm-100Threads-60Min-part-noStale 211 48,32,16,8,4,2,1 MR1 BG-skewed-200-4 true 10.0.0.120

 ./runValidatorThreadsMultiServers.sh /home/mr1/BG-10K-100fpm-100Threads-60Min-part-noStale 211 48,32,16,8,4,2,1 MR1 BG-skewed-200-5 true 10.0.0.120
