#!/bin/bash


nums=$(echo $2 | tr "," "\n")

for i in $nums
do
    
echo "Running Validator with "$i
./validScript.sh $1 $i $3 $4



done






