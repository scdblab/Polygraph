#!/bin/bash



cd /home/mr1/StaleMeter




 ./runValidatorThreadsMultiServers.sh /home/mr1/BG-10K-100fpm-100Threads-60Min-part-noStale-uniform-readonly 0 2,1 MR1 BG-uniformRead1-0 true 10.0.0.120

 ./runValidatorThreadsMultiServers.sh /home/mr1/BG-10K-100fpm-100Threads-60Min-part-noStale-uniform-readonly 0 2,1 MR1 BG-uniformRead2-0 true 10.0.0.120

 ./runValidatorThreadsMultiServers.sh /home/mr1/BG-10K-100fpm-100Threads-60Min-part-noStale-uniform-readonly 0 2,1 MR1 BG-uniformRead3-0 true 10.0.0.120

 ./runValidatorThreadsMultiServers.sh /home/mr1/BG-10K-100fpm-100Threads-60Min-part-noStale-uniform-readonly 0 2,1 MR1 BG-uniformRead4-0 true 10.0.0.120

 ./runValidatorThreadsMultiServers.sh /home/mr1/BG-10K-100fpm-100Threads-60Min-part-noStale-uniform-readonly 0 2,1 MR1 BG-uniformRead5-0 true 10.0.0.120
